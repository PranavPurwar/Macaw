package dev.pranav.macaw.ui.file

import java.nio.file.Path

sealed class FilePreviewState {
    object None : FilePreviewState()
    data class Audio(val file: Path) : FilePreviewState()
}
