package dev.pranav.macaw.model

import java.io.File

enum class FileType {
    IMAGE,
    VIDEO,
    AUDIO,
    ARCHIVE,
    TEXT,
    CODE,
    UNKNOWN,
}

val IMAGE_FORMATS: Set<String> =
    setOf("png", "jpg", "jpeg", "svg", "webp", "gif", "bmp", "avif", "ico", "tiff", "heic", "heif")

// Video formats supported by coil
val VIDEO_FORMATS: Set<String> = setOf("mp4", "3gp", "mkv", "webm")

val AUDIO_FORMATS: Set<String> = setOf("mp3", "aac", "m4a")

val ARCHIVE_FORMATS: Set<String> = setOf("zip", "tar", "gz", "aab")

val TEXT_FORMATS: Set<String> = setOf("txt", "json", "md", "properties", "ini", "csv")

val CODE_FORMATS: Set<String> =
    setOf(
        "bash",
        "c",
        "cpp",
        "cs",
        "css",
        "dart",
        "go",
        "h",
        "hpp",
        "html",
        "java",
        "js",
        "json",
        "kts",
        "kt",
        "lua",
        "md",
        "php",
        "pl",
        "properties",
        "py",
        "r",
        "rb",
        "rs",
        "scala",
        "sh",
        "sql",
        "swift",
        "ts",
        "vb",
        "xml",
        "yaml",
        "yml",
        "s",
        "asm",
    )

fun File.getFileType(): FileType {
    return when (this.extension.lowercase()) {
        in IMAGE_FORMATS -> FileType.IMAGE
        in VIDEO_FORMATS -> FileType.VIDEO
        in AUDIO_FORMATS -> FileType.AUDIO
        in ARCHIVE_FORMATS -> FileType.ARCHIVE
        in TEXT_FORMATS -> FileType.TEXT
        in CODE_FORMATS -> FileType.CODE
        else -> FileType.UNKNOWN
    }
}
