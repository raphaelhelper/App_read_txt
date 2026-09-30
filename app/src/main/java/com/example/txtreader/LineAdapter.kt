package com.example.txtreader

import android.graphics.Color
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LineAdapter : RecyclerView.Adapter<LineAdapter.VH>() {

    var lines: List<String> = emptyList()
    var query: String = ""
    var current: Int = -1

    class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_line, parent, false) as TextView
        return VH(tv)
    }

    override fun getItemCount() = lines.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val text = lines[position]
        if (query.isEmpty()) {
            holder.tv.text = text
            return
        }
        val sp = SpannableString(text)
        val bg = if (position == current) 0xFFFF9800.toInt() else 0xFFFFEB3B.toInt()
        var i = text.indexOf(query, 0, ignoreCase = true)
        while (i >= 0) {
            val end = i + query.length
            sp.setSpan(BackgroundColorSpan(bg), i, end, 0)
            sp.setSpan(ForegroundColorSpan(Color.BLACK), i, end, 0)
            i = text.indexOf(query, end, ignoreCase = true)
        }
        holder.tv.text = sp
    }
}
