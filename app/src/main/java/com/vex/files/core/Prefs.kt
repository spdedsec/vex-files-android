package com.vex.files.core

import android.content.Context
import org.json.JSONArray

class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("vex_files", Context.MODE_PRIVATE)

    fun favorites(): MutableSet<String> = prefs.getStringSet("favorites", emptySet())?.toMutableSet() ?: mutableSetOf()

    fun setFavorites(values: Set<String>) {
        prefs.edit().putStringSet("favorites", values).apply()
    }

    fun recents(): List<String> {
        val raw = prefs.getString("recents", "[]") ?: "[]"
        val array = JSONArray(raw)
        return List(array.length()) { array.getString(it) }
    }

    fun addRecent(path: String) {
        val list = recents().toMutableList()
        list.remove(path)
        list.add(0, path)
        val keep = list.take(12)
        val array = JSONArray()
        keep.forEach(array::put)
        prefs.edit().putString("recents", array.toString()).apply()
    }
}
