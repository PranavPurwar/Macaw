package dev.pranav.macaw.model

import java.io.File
import java.util.UUID

data class TabData(
    val id: UUID = UUID.randomUUID(),
    val initialRootDir: File,
    var currentPath: File = initialRootDir,
    var isBookmarked: Boolean = false
)

