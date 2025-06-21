package dev.pranav.filemanager.ui.home

import java.io.File

sealed class FilePreviewState {
    object None : FilePreviewState()
    data class Audio(val file: File) : FilePreviewState()
}

