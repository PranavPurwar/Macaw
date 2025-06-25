package dev.pranav.macaw.util

import android.content.Context
import android.content.SharedPreferences

object BookmarksManager {
    private const val PREFS_NAME = "macaw_bookmarks"
    private const val BOOKMARKS_KEY = "bookmarks"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBookmarkedPaths(context: Context): Set<String> {
        return getPrefs(context).getStringSet(BOOKMARKS_KEY, emptySet()) ?: emptySet()
    }

    fun addBookmark(context: Context, path: String) {
        val bookmarks = getBookmarkedPaths(context).toMutableSet()
        bookmarks.add(path)
        getPrefs(context).edit().putStringSet(BOOKMARKS_KEY, bookmarks).apply()
    }

    fun removeBookmark(context: Context, path: String) {
        val bookmarks = getBookmarkedPaths(context).toMutableSet()
        bookmarks.remove(path)
        getPrefs(context).edit().putStringSet(BOOKMARKS_KEY, bookmarks).apply()
    }

    fun isBookmarked(context: Context, path: String): Boolean {
        return getBookmarkedPaths(context).contains(path)
    }
}

