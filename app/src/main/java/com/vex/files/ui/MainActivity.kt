package com.vex.files.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vex.files.R
import com.vex.files.core.FileItem
import com.vex.files.core.FileOps
import com.vex.files.core.FormatUtils
import com.vex.files.core.FsDirectory
import com.vex.files.core.FsFile
import com.vex.files.core.Prefs
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val bg = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var list: RecyclerView
    private lateinit var adapter: FileAdapter
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var actionBar: LinearLayout
    private lateinit var homeBar: LinearLayout
    private lateinit var search: EditText
    private lateinit var pathView: TextView
    private lateinit var prefs: Prefs
    private var current = Environment.getExternalStorageDirectory()
    private var sortMode = SortMode.NAME
    private var descending = false
    private var searchMode = false

    private enum class SortMode { NAME, SIZE, DATE, TYPE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestEdgeToEdge()
        prefs = Prefs(this)
        buildUi()
        if (!Environment.isExternalStorageManager()) {
            showStorageGate()
        } else {
            showDirectory(current)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::pathView.isInitialized && Environment.isExternalStorageManager()) showDirectory(current, animate = false)
    }

    private fun requestEdgeToEdge() {
        if (android.os.Build.VERSION.SDK_INT >= 35) {
            WindowCompatCompat.enable(window)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.BLACK
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 10, 10))
            setPadding(0, 0, 0, 0)
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(36), dp(16), dp(10))
        }
        val topRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        title = label("VEX FILES", 24f, Color.rgb(241, 240, 235), true)
        subtitle = label("FAST / LOCAL / NO CLOUD", 10f, Color.rgb(167, 164, 156), false)
        topRow.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        val searchBtn = smallButton("SEARCH") { toggleSearch() }
        topRow.addView(searchBtn)
        val moreBtn = smallButton("MENU") { showMenu() }
        topRow.addView(space(dp(6), 1))
        topRow.addView(moreBtn)
        top.addView(topRow)
        pathView = label("/", 12f, Color.rgb(167, 164, 156), false).apply { typeface = Typeface.MONOSPACE }
        top.addView(pathView)
        root.addView(top)

        search = EditText(this).apply {
            hint = "search current folder"
            hintTextColor = Color.rgb(120, 118, 112)
            setTextColor(Color.rgb(241, 240, 235))
            textSize = 14f
            setSingleLine(true)
            setPadding(dp(14), 0, dp(14), 0)
            background = android.graphics.drawable.ColorDrawable(Color.rgb(24, 24, 23))
            visibility = View.GONE
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(dp(16), 0, dp(16), dp(8)) })
        search.addTextChangedListener(SimpleWatcher { refreshList(it) })

        list = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        adapter = FileAdapter(
            onClick = { item -> if (item.isDir) showDirectory(item.file) else openFile(item.file) },
            onLongClick = { item ->
                if (!adapter.hasSelection()) adapter.toggle(item)
                else adapter.toggle(item)
                updateActionBar()
            }
        )
        list.adapter = adapter
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))

        homeBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.rgb(18, 18, 17))
        }
        homeBar.addView(bottomButton("HOME") { goHome() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        homeBar.addView(bottomButton("BACK") { goBack() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        homeBar.addView(bottomButton("NEW") { createFolder() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(homeBar)

        actionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.rgb(228, 30, 43))
            visibility = View.GONE
        }
        root.addView(actionBar)
        setContentView(root)
        root.viewTreeObserver.addOnGlobalLayoutListener { }
    }

    private fun showDirectory(dir: File, animate: Boolean = true) {
        if (!dir.exists() || !dir.isDirectory) return
        current = dir
        prefs.addRecent(dir.absolutePath)
        pathView.text = prettyPath(dir)
        search.text.clear()
        loadFiles("")
        if (animate) list.animate().translationX(0f).alpha(1f).setDuration(120).start()
        updateActionBar()
    }

    private fun refreshList(query: String) = loadFiles(query)

    private fun loadFiles(query: String) {
        val base = current
        bg.execute {
            val files = base.listFiles()?.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }?.sortedWith(comparator()) ?: emptyList()
            val mapped = files.map { if (it.isDirectory) FsDirectory(it) as FileItem else FsFile(it) }
            main.post { adapter.submit(mapped); pathView.text = "${prettyPath(base)}   /   ${mapped.size}" }
        }
    }

    private fun comparator(): Comparator<File> = Comparator { a, b ->
        val value = when (sortMode) {
            SortMode.NAME -> a.name.lowercase(Locale.US).compareTo(b.name.lowercase(Locale.US))
            SortMode.SIZE -> a.length().compareTo(b.length())
            SortMode.DATE -> a.lastModified().compareTo(b.lastModified())
            SortMode.TYPE -> a.extension.lowercase(Locale.US).compareTo(b.extension.lowercase(Locale.US))
        }
        if (descending) -value else value
    }.let { base -> Comparator { a, b -> if (a.isDirectory != b.isDirectory) if (a.isDirectory) -1 else 1 else base.compare(a, b) } }

    private fun goBack() {
        if (current.parentFile != null && current.absolutePath != Environment.getExternalStorageDirectory().absolutePath) showDirectory(current.parentFile!!)
        else goHome()
    }

    private fun goHome() {
        current = Environment.getExternalStorageDirectory()
        val items = current.listFiles()?.toList().orEmpty()
        pathView.text = "STORAGE / INTERNAL   ${FormatUtils.storage(current.absolutePath).let { "${FormatUtils.bytes(it.second)} FREE / ${it.third}% USED" }}"
        adapter.submit(items.sortedWith(comparator()).map { if (it.isDirectory) FsDirectory(it) else FsFile(it) })
        search.visibility = View.GONE
        searchMode = false
        updateActionBar()
    }

    private fun createFolder() {
        val input = EditText(this).apply { hint = "folder name"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        dialog("NEW DIRECTORY", input, "CREATE") {
            val name = input.text.toString().trim()
            if (name.isBlank() || name.contains("/")) return@dialog
            val dir = File(current, name)
            if (dir.exists()) toast("Already exists") else if (dir.mkdirs()) showDirectory(current) else toast("Could not create")
        }
    }

    private fun toggleSearch() {
        searchMode = !searchMode
        search.visibility = if (searchMode) View.VISIBLE else View.GONE
        if (searchMode) search.requestFocus()
        else search.text.clear()
    }

    private fun showMenu() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(8), dp(18), dp(8)) }
        val sort = label("SORT", 11f, Color.rgb(167, 164, 156), true)
        box.addView(sort)
        listOf("NAME", "SIZE", "DATE", "TYPE").forEach { s ->
            val b = menuRow(s) {
                sortMode = SortMode.valueOf(s)
                descending = if (sortMode == SortMode.NAME) false else descending
                loadFiles(search.text.toString())
            }
            box.addView(b)
        }
        box.addView(menuRow(if (descending) "ORDER: DESC" else "ORDER: ASC") {
            descending = !descending
            loadFiles(search.text.toString())
        })
        box.addView(menuRow("FAVORITE FOLDER") { addFavorite() })
        box.addView(menuRow("RECENT") { showRecent() })
        box.addView(menuRow("STORAGE ACCESS") { openAccessSettings() })
        dialogView("MENU", box)
    }

    private fun showRecent() {
        val recents = prefs.recents().map(::File).filter { it.exists() }
        adapter.submit(recents.map { if (it.isDirectory) FsDirectory(it) else FsFile(it) })
        pathView.text = "RECENT"
    }

    private fun addFavorite() {
        val favorites = prefs.favorites()
        val path = current.absolutePath
        if (!favorites.add(path)) favorites.remove(path)
        prefs.setFavorites(favorites)
        toast(if (favorites.contains(path)) "Added to favorites" else "Removed from favorites")
    }

    private fun updateActionBar() {
        actionBar.removeAllViews()
        if (!adapter.hasSelection()) {
            actionBar.visibility = View.GONE
            homeBar.visibility = View.VISIBLE
            return
        }
        actionBar.visibility = View.VISIBLE
        homeBar.visibility = View.GONE
        val count = label("${adapter.selectionCount()} SEL", 12f, Color.WHITE, true)
        actionBar.addView(count, LinearLayout.LayoutParams(0, dp(48), 1f))
        actionBar.addView(action("COPY") { chooseDestination(false) })
        actionBar.addView(action("MOVE") { chooseDestination(true) })
        if (adapter.selectionCount() == 1) actionBar.addView(action("REN") { renameSelected() })
        actionBar.addView(action("ZIP") { archiveSelected() })
        actionBar.addView(action("SHARE") { shareSelected() })
        actionBar.addView(action("DEL") { deleteSelected() })
    }

    private fun chooseDestination(move: Boolean) {
        val dirs = listDirectories(current)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(6), dp(12), dp(8)) }
        dirs.forEach { dir -> box.addView(menuRow("/ ${dir.name}") { executeCopyMove(dir, move) }) }
        box.addView(menuRow("CHOOSE OTHER FOLDER") { openTreePicker() })
        dialogView(if (move) "MOVE TO" else "COPY TO", box)
    }

    private fun executeCopyMove(destination: File, move: Boolean) {
        val files = adapter.selectedFiles()
        adapter.clearSelection(); updateActionBar()
        bg.execute {
            runCatching { files.forEach { if (move) FileOps.move(it, destination) else FileOps.copy(it, destination) } }
                .onSuccess { main.post { toast(if (move) "Moved" else "Copied"); loadFiles(search.text.toString()) } }
                .onFailure { err -> main.post { toast(err.message ?: "Operation failed") } }
        }
    }


    private fun renameSelected() {
        val files = adapter.selectedFiles()
        if (files.size != 1) return
        val source = files.first()
        val input = EditText(this).apply {
            setText(source.name)
            setSelection(text.length)
            setTextColor(Color.WHITE)
        }
        dialog("RENAME", input, "SAVE") {
            val newName = input.text.toString().trim()
            if (newName.isBlank() || newName.contains("/")) {
                toast("Invalid name")
                return@dialog
            }
            val target = File(source.parentFile, newName)
            if (target.exists()) {
                toast("Already exists")
                return@dialog
            }
            if (source.renameTo(target)) {
                adapter.clearSelection()
                updateActionBar()
                loadFiles(search.text.toString())
                toast("Renamed")
            } else {
                toast("Could not rename")
            }
        }
    }

    private fun archiveSelected() {
        val files = adapter.selectedFiles()
        if (files.isEmpty()) return
        val defaultName = if (files.size == 1) files.first().name + ".zip" else "archive.zip"
        val input = EditText(this).apply { setText(defaultName); setTextColor(Color.WHITE) }
        dialog("CREATE ZIP", input, "ARCHIVE") {
            val name = input.text.toString().trim().ifBlank { defaultName }.let { if (it.endsWith(".zip")) it else "$it.zip" }
            val out = uniqueFile(File(current, name))
            adapter.clearSelection(); updateActionBar()
            bg.execute {
                runCatching { FileOps.createZip(files, out) }
                    .onSuccess { main.post { toast("Created ${out.name}"); loadFiles(search.text.toString()) } }
                    .onFailure { e -> main.post { toast(e.message ?: "Archive failed") } }
            }
        }
    }

    private fun shareSelected() {
        val files = adapter.selectedFiles()
        if (files.size != 1) { toast("Select one file to share"); return }
        val file = files.first()
        if (file.isDirectory) { toast("Share a file or archive"); return }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = FormatUtils.mime(file.name)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    }

    private fun deleteSelected() {
        val files = adapter.selectedFiles()
        if (files.isEmpty()) return
        dialogText("DELETE ${files.size} ITEM(S)", "This is permanent. No recycle bin in v0.1.", "DELETE") {
            adapter.clearSelection(); updateActionBar()
            bg.execute {
                val ok = files.all(FileOps::delete)
                main.post { toast(if (ok) "Deleted" else "Some items could not be deleted"); loadFiles(search.text.toString()) }
            }
        }
    }

    private fun openFile(file: File) {
        prefs.addRecent(file.absolutePath)
        if (file.extension.equals("zip", true)) {
            extractZipFlow(file)
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, FormatUtils.mime(file.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }.onFailure { toast("No app can open this file") }
    }

    private fun extractZipFlow(file: File) {
        dialogText("OPEN ZIP", "Extract it into a new folder next to the archive?", "EXTRACT") {
            bg.execute {
                val destination = File(current, file.nameWithoutExtension).let(::uniqueDir)
                runCatching { FileOps.extractZip(file, destination) }
                    .onSuccess { main.post { toast("Extracted to ${destination.name}"); showDirectory(destination) } }
                    .onFailure { e -> main.post { toast(e.message ?: "Extract failed") } }
            }
        }
    }

    private fun openAccessSettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun openTreePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }
        startActivityForResult(intent, 17)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 17 && resultCode == Activity.RESULT_OK && data?.data != null) toast("SAF folder selected: ${data.data}")
    }

    private fun showStorageGate() {
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(12), dp(22), dp(8)) }
        body.addView(label("FULL STORAGE ACCESS", 11f, Color.rgb(228, 30, 43), true))
        body.addView(label("VEX Files is designed as a primary local file manager. Android requires a special storage-management permission for broad filesystem access. Grant it once, then the app stays local and offline.", 15f, Color.rgb(241, 240, 235), false).apply { setPadding(0, dp(10), 0, dp(14)) })
        body.addView(menuRow("OPEN SYSTEM ACCESS SETTINGS") { openAccessSettings() })
        body.addView(menuRow("USE LIMITED MODE") { showDirectory(Environment.getExternalStorageDirectory()) })
        dialogView("FIRST RUN", body)
    }

    private fun dialog(titleText: String, content: View, actionText: String, action: () -> Unit) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), 0, dp(18), dp(10)) }
        box.addView(content)
        dialogView(titleText, box, actionText, action)
    }

    private fun dialogText(titleText: String, text: String, actionText: String, action: () -> Unit) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(10), dp(18), dp(10)) }
        box.addView(label(text, 14f, Color.rgb(241, 240, 235), false))
        dialogView(titleText, box, actionText, action)
    }

    private fun dialogView(titleText: String, content: View, actionText: String? = null, action: (() -> Unit)? = null) {
        val dialog = android.app.Dialog(this)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(18, 18, 17)) }
        shell.addView(label(titleText, 18f, Color.rgb(241, 240, 235), true).apply { setPadding(dp(18), dp(16), dp(18), dp(10)) })
        shell.addView(content)
        val actions = LinearLayout(this).apply { gravity = Gravity.END; setPadding(dp(12), dp(6), dp(12), dp(12)) }
        actions.addView(menuRow("CLOSE") { dialog.dismiss() })
        if (actionText != null && action != null) actions.addView(menuRow(actionText) { dialog.dismiss(); action() })
        shell.addView(actions)
        dialog.setContentView(shell)
        dialog.setOnShowListener {
            val w = dialog.window
            w?.setBackgroundDrawableResource(android.R.color.transparent)
            w?.setLayout((resources.displayMetrics.widthPixels * .92).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * .92).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        typeface = Typeface.create(if (bold) Typeface.MONOSPACE else "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun smallButton(text: String, action: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        textSize = 11f
        setTextColor(Color.rgb(241, 240, 235))
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setPadding(dp(8), dp(8), dp(8), dp(8))
        setOnClickListener { action() }
    }

    private fun bottomButton(text: String, action: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        textSize = 11f
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(241, 240, 235))
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setOnClickListener { action() }
    }

    private fun action(text: String, action: () -> Unit): TextView = bottomButton(text, action).apply { setTextColor(Color.WHITE) }

    private fun menuRow(text: String, action: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(Color.rgb(241, 240, 235))
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setPadding(dp(8), dp(12), dp(8), dp(12))
        setOnClickListener { action() }
        isClickable = true
    }

    private fun prettyPath(file: File): String {
        val root = Environment.getExternalStorageDirectory().absolutePath
        if (file.absolutePath == root) return "INTERNAL STORAGE /"
        return "INTERNAL STORAGE / " + file.absolutePath.removePrefix(root).trim('/').replace('/', ' ')
    }

    private fun listDirectories(dir: File): List<File> = dir.listFiles()?.filter(File::isDirectory)?.sortedBy { it.name.lowercase(Locale.US) }.orEmpty()

    private fun uniqueFile(file: File): File {
        if (!file.exists()) return file
        var i = 2
        while (true) {
            val candidate = File(file.parentFile, "${file.nameWithoutExtension}-$i.${file.extension}")
            if (!candidate.exists()) return candidate
            i++
        }
    }

    private fun uniqueDir(dir: File): File {
        if (!dir.exists()) return dir
        var i = 2
        while (true) {
            val candidate = File(dir.parentFile, "${dir.name}-$i")
            if (!candidate.exists()) return candidate
            i++
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    private fun space(w: Int, h: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(w, h) }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        bg.shutdownNow()
        super.onDestroy()
    }
}

private object WindowCompatCompat {
    fun enable(window: Window) {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.setSystemBarsAppearance(0, WindowInsetsControllerCompatCompat.LIGHT_STATUS_BARS)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
    }
}

private object WindowInsetsControllerCompatCompat {
    const val LIGHT_STATUS_BARS = 8
}

private fun android.widget.EditText.addTextChangedListener(watcher: SimpleWatcher) {
    addTextChangedListener(watcher.asWatcher())
}

private class SimpleWatcher(private val onText: (String) -> Unit) {
    fun asWatcher() = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onText(s?.toString().orEmpty()) }
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}
