package com.vex.files.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vex.files.core.FileItem
import com.vex.files.core.FormatUtils
import java.io.File

class FileAdapter(
    private val onClick: (FileItem) -> Unit,
    private val onLongClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.Holder>() {

    private lateinit var context: android.content.Context

    private val items = mutableListOf<FileItem>()
    private val selected = linkedSetOf<String>()

    fun submit(list: List<FileItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun selectedFiles(): List<File> = items.filter { selected.contains(it.file.absolutePath) }.map { it.file }

    fun toggle(item: FileItem) {
        val path = item.file.absolutePath
        if (!selected.add(path)) selected.remove(path)
        notifyItemChanged(items.indexOf(item))
    }

    fun clearSelection() {
        selected.clear()
        notifyDataSetChanged()
    }

    fun hasSelection(): Boolean = selected.isNotEmpty()

    fun selectionCount(): Int = selected.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        context = parent.context
        val row = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(12), dp(8))
            minimumHeight = dp(58)
            background = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        }
        return Holder(row)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class Holder(private val row: LinearLayout) : RecyclerView.ViewHolder(row) {
        private val tagView = TextView(row.context).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(228, 30, 43))
        }
        private val name = TextView(row.context).apply {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textSize = 16f
            setTextColor(Color.rgb(241, 240, 235))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        }
        private val meta = TextView(row.context).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textSize = 11f
            setTextColor(Color.rgb(167, 164, 156))
            maxLines = 1
        }
        private val textBox = LinearLayout(row.context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        private val arrow = TextView(row.context).apply {
            text = "›"
            textSize = 24f
            setTextColor(Color.rgb(167, 164, 156))
        }

        init {
            row.addView(tagView, LinearLayout.LayoutParams(dp(38), dp(38)))
            row.addView(space(dp(12), 1))
            textBox.addView(name)
            textBox.addView(meta)
            row.addView(textBox, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(arrow, LinearLayout.LayoutParams(dp(24), dp(40)))
        }

        fun bind(item: FileItem) {
            val isSelected = selected.contains(item.file.absolutePath)
            tagView.text = if (item.isDir) "/" else extensionTag(item.file)
            tagView.setBackgroundColor(if (item.isDir) Color.rgb(241, 240, 235) else Color.rgb(228, 30, 43))
            tagView.setTextColor(if (item.isDir) Color.rgb(10, 10, 10) else Color.WHITE)
            name.text = item.name
            meta.text = if (item.isDir) "DIRECTORY   ${FormatUtils.date(item.file.lastModified())}" else "${FormatUtils.bytes(item.size)}   ${FormatUtils.date(item.file.lastModified())}"
            arrow.text = if (item.isDir) "›" else ""
            row.setBackgroundColor(if (isSelected) Color.rgb(47, 26, 28) else Color.TRANSPARENT)
            row.setOnClickListener {
                if (hasSelection()) {
                    toggle(item)
                } else onClick(item)
            }
            row.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }

    private fun extensionTag(file: File): String = file.extension.take(3).uppercase().ifBlank { "FILE" }
    private fun dp(v: Int): Int = (v * rowResources.density).toInt()
    private fun space(w: Int, h: Int): View = View(holderContext()).apply { layoutParams = LinearLayout.LayoutParams(w, h) }
    private val rowResources get() = android.content.res.Resources.getSystem().displayMetrics
    private fun holderContext() = context
}
