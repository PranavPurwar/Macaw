package dev.pranav.filemanager.model

import java.io.File

enum class FileType {
    IMAGE,
    VIDEO,
    AUDIO,
    ARCHIVE,
    TEXT,
    CODE,
    UNKNOWN
}

val IMAGE_FORMATS = listOf(
    "png", "jpg", "jpeg", "svg", "webp"
)

val VIDEO_FORMATS = listOf(
    "mp4"
)

val AUDIO_FORMATS = listOf(
    "mp3", "aac"
)

val ARCHIVE_FORMATS = listOf(
    "zip", "tar", "gz", "aab"
)

val EDITOR_FORMATS = listOf(
    "txt", "json", "md", "properties", "ini"
)

val CODE_FORMATS = listOf(
    "bash", "c", "cpp", "cs", "css", "dart", "go", "h", "hpp", "html", "java", "js", "json", "kts", "kt", "lua", "md", "php", "pl", "properties", "py", "r", "rb", "rs", "scala", "sh", "sql", "swift", "ts", "vb", "xml", "yaml", "yml"
)

fun File.getFileType(): FileType {
    if (extension in EDITOR_FORMATS) return FileType.TEXT
    if (extension in IMAGE_FORMATS) return FileType.IMAGE
    if (extension in VIDEO_FORMATS) return FileType.VIDEO
    if (extension in AUDIO_FORMATS) return FileType.AUDIO
    if (extension in ARCHIVE_FORMATS) return FileType.ARCHIVE
    if (extension in CODE_FORMATS) return FileType.CODE

    return FileType.UNKNOWN
}

