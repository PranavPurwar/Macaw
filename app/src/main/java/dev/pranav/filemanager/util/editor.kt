package dev.pranav.filemanager.util

import dev.pranav.filemanager.R

fun mapFontNameToFontResource(fontName: String): Int? {
    return when (fontName) {
        "JetBrains Mono" -> R.font.jetbrains_mono
        // todo: Add more fonts as needed
//        "Fira Code" -> dev.pranav.filemanager.R.font.fira_code
//        "Source Code Pro" -> dev.pranav.filemanager.R.font.source_code_pro
//        "Roboto Mono" -> dev.pranav.filemanager.R.font.roboto_mono
//        "Noto Sans Mono" -> dev.pranav.filemanager.R.font.noto_sans_mono
        else -> null
    }
}
