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

    // vùng đang chọn để copy (-1 = không chọn)
    var selStart: Int = -1
    var selEnd: Int = -1

    var onLineClick: (Int) -> Unit = {}
    var onLineLongClick: (Int) -> Unit = {}

    class VH(val tv: TextView) : RecyclerView.ViewHolder(tv) {
        val defColor: Int = tv.currentTextColor
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_line, parent, false) as TextView
        val holder = VH(tv)
        tv.setOnClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) onLineClick(p)
        }
        tv.setOnLongClickListener {
            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) onLineLongClick(p)
            true
        }
        return holder
    }

    override fun getItemCount() = lines.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val selected = selStart >= 0 &&
            position >= minOf(selStart, selEnd) && position <= maxOf(selStart, selEnd)
        if (selected) {
            holder.tv.setBackgroundColor(0xFFBBDEFB.toInt())
            holder.tv.setTextColor(Color.BLACK)
        } else {
            holder.tv.setBackgroundColor(Color.TRANSPARENT)
            holder.tv.setTextColor(holder.defColor)
        }

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
