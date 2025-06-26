package dev.pranav.macaw.util

data class FileEntry(
    val name: String,
    val details: String,
    val isDirectory: Boolean,
    val lastModified: Long,
    val size: Long,
    val absolutePath: String
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")
}

object FileJNI {

    init {
        System.loadLibrary("fileops")
    }

    external fun getOrderedChildren(
        path: String,
        sortOrder: Int = SortOrder.NAME_ASCENDING.ordinal
    ): Array<FileEntry>

    external fun getFileDetails(path: String): String

    external fun getLastModifiedFormatted(path: String, format: String = "MMM dd, hh:mm a"): String

    external fun getSizeString(path: String): String

    external fun getFolderContentsCount(path: String): String
}
