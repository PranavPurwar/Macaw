package dev.pranav.macaw.util

import dev.pranav.macaw.R

fun mapFontNameToFontResource(fontName: String): Int? {
    return when (fontName) {
        "JetBrains Mono" -> R.font.jetbrains_mono
        // todo: Add more fonts as needed
//        "Fira Code" -> dev.pranav.macaw.R.font.fira_code
//        "Source Code Pro" -> dev.pranav.macaw.R.font.source_code_pro
//        "Roboto Mono" -> dev.pranav.macaw.R.font.roboto_mono
//        "Noto Sans Mono" -> dev.pranav.macaw.R.font.noto_sans_mono
        else -> null
    }
}
