package com.example.txtreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.txtreader.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MAX_LEN = 2000 // dòng dài hơn sẽ được cắt thành nhiều đoạn
    }

    private lateinit var b: ActivityMainBinding
    private lateinit var lm: LinearLayoutManager
    private val adapter = LineAdapter()

    private var lines: List<String> = emptyList()
    private var curUri: Uri? = null
    private var matches = IntArray(0)
    private var cur = -1

    private val picker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            load(uri, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        lm = LinearLayoutManager(this)
        b.rv.layoutManager = lm
        b.rv.adapter = adapter

        b.btnOpen.setOnClickListener { picker.launch(arrayOf("*/*")) }
        b.btnNext.setOnClickListener { step(1) }
        b.btnPrev.setOnClickListener { step(-1) }
        b.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                search()
                true
            } else false
        }

        val shared = intent?.data
        if (shared != null) {
            load(shared, 0)
        } else {
            val prefs = getSharedPreferences("p", MODE_PRIVATE)
            prefs.getString("uri", null)?.let { load(Uri.parse(it), prefs.getInt("pos", 0)) }
        }
    }

    override fun onPause() {
        super.onPause()
        val u = curUri ?: return
        getSharedPreferences("p", MODE_PRIVATE).edit()
            .putString("uri", u.toString())
            .putInt("pos", lm.findFirstVisibleItemPosition().coerceAtLeast(0))
            .apply()
    }

    private fun load(uri: Uri, startPos: Int) {
        b.progress.visibility = View.VISIBLE
        b.tvStatus.text = "Đang tải..."
        lifecycleScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { readLines(uri) }
                lines = res
                curUri = uri
                matches = IntArray(0)
                cur = -1
                adapter.lines = res
                adapter.query = ""
                adapter.current = -1
                adapter.notifyDataSetChanged()
                lm.scrollToPositionWithOffset(
                    startPos.coerceIn(0, maxOf(0, res.size - 1)), 0
                )
                b.tvStatus.text = "${res.size} dòng"
            } catch (e: Throwable) {
                b.tvStatus.text = "Lỗi"
                Toast.makeText(
                    this@MainActivity, "Không mở được: ${e.message}", Toast.LENGTH_LONG
                ).show()
            } finally {
                b.progress.visibility = View.GONE
            }
        }
    }

    private fun readLines(uri: Uri): List<String> {
        val out = ArrayList<String>()
        val ins = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("không đọc được file")
        ins.use {
            BufferedReader(InputStreamReader(it, Charsets.UTF_8), 1 shl 16).forEachLine { raw ->
                var s = raw
                if (out.isEmpty() && s.startsWith("\uFEFF")) s = s.substring(1)
                if (s.length <= MAX_LEN) {
                    out.add(s)
                } else {
                    var i = 0
                    while (i < s.length) {
                        out.add(s.substring(i, minOf(i + MAX_LEN, s.length)))
                        i += MAX_LEN
                    }
                }
            }
        }
        return out
    }

    private fun search() {
        val q = b.etSearch.text.toString().trim()
        if (q.isEmpty() || lines.isEmpty()) return
        val snapshot = lines
        lifecycleScope.launch {
            val found = withContext(Dispatchers.Default) {
                val r = ArrayList<Int>()
                for (i in snapshot.indices) {
                    if (snapshot[i].contains(q, ignoreCase = true)) r.add(i)
                }
                r.toIntArray()
            }
            matches = found
            adapter.query = q
            if (found.isEmpty()) {
                cur = -1
                adapter.current = -1
                adapter.notifyDataSetChanged()
                b.tvStatus.text = "Không thấy \"$q\""
                return@launch
            }
            val first = lm.findFirstVisibleItemPosition()
            val start = found.indexOfFirst { it >= first }.let { if (it < 0) 0 else it }
            goTo(start)
        }
    }

    private fun step(d: Int) {
        val q = b.etSearch.text.toString().trim()
        if (q != adapter.query || matches.isEmpty()) {
            search()
            return
        }
        goTo((cur + d + matches.size) % matches.size)
    }

    private fun goTo(i: Int) {
        cur = i
        adapter.current = matches[i]
        lm.scrollToPositionWithOffset(matches[i], 0)
        adapter.notifyDataSetChanged()
        b.tvStatus.text = "${i + 1}/${matches.size} kết quả"
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(b.etSearch.windowToken, 0)
    }
}
