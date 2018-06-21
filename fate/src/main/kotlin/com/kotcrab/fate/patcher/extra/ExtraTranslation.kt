package com.kotcrab.fate.patcher.extra

import com.kotcrab.fate.patcher.Translation
import java.io.File

/** @author Kotcrab */
class ExtraTranslation(jpFile: File, enFile: File = jpFile.resolveSibling("script-translation.txt"),
                       stripNewLine: Boolean = false,
                       overrides: Map<Int, String> = mapOf())
    : Translation(jpFile, enFile, stripNewLine, overrides) {

    fun getTranslation(index: Int, offset: Int = 0): String {
        return getRawTranslation(index, offset, true)
                .replace("\r\n", "\n")
                .replace("’", "'")
                .replace("“", "\"")
                .replace("”", "\"")
    }
}
