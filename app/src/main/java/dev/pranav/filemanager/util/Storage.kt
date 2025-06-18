package dev.pranav.filemanager.util

import android.annotation.SuppressLint
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import kotlin.math.log10
import kotlin.math.pow

fun File.details(): String {
    if (isFile) {
        return getLastModifiedDate() + " | " + sizeString()
    }
    if (!canRead()) return "Insufficient Permissions"
    val children = listFiles()!!
    val filesNo = children.filter { it.isFile }.size
    val foldersNo = children.size - filesNo

    return foldersNo.toString() + " " + if (foldersNo == 1) "folder" else "folders" + ", " + filesNo.toString() + " " + if (filesNo == 1) "file" else "files"
}

@SuppressLint("SimpleDateFormat")
fun File.getLastModifiedDate(dateFormat: String = "MMM dd, hh:mm a"): String {
    return SimpleDateFormat(dateFormat).format(lastModified())
}

fun File.sizeString(): String {
    if (this.length() <= 0) return "0 B"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (log10(this.length().toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(
        this.length() / 1024.0.pow(digitGroups.toDouble())
    ) + " " + units[digitGroups]
}

fun File.orderedChildren(): List<File> {
    val children = listFiles() ?: return listOf()
    val files = mutableListOf<File>()
    val directories = mutableListOf<File>()
    for (file in children) {
        if (file.isFile) {
            files.add(file)
        } else {
            directories.add(file)
        }
    }
    files.sortBy { it.name }
    directories.sortBy { it.name }
    return directories + files
}

