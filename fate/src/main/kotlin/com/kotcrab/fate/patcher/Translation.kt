package com.kotcrab.fate.patcher

import java.io.File

abstract class Translation(jpFile: File, enFile: File = jpFile.resolveSibling("script-translation.txt"),
                           stripNewLine: Boolean = false,
                           private val overrides: Map<Int, String> = mapOf()) {
    val jpTexts = processTranslationFile(jpFile, stripNewLine)
    val enTexts = processTranslationFile(enFile, stripNewLine)

    init {
        if (jpTexts.size != enTexts.size) error("entry count mismatch")
    }

    fun isTranslated(index: Int, offset: Int = 0): Boolean {
        val enText = enTexts[offset + index]
        return !enText.isBlank()
    }

    protected fun getRawTranslation(index: Int, offset: Int = 0, allowBlank: Boolean = false): String {
        val overrideText = overrides[offset + index]
        if (overrideText != null) return overrideText
        val enText = enTexts[offset + index]
        if (enText.isBlank() && allowBlank == false) return jpTexts[offset + index]
        return enText
    }

    fun getOriginal(index: Int, offset: Int = 0): String {
        return jpTexts[offset + index]
    }
}

private fun processTranslationFile(file: File, stripNewLine: Boolean): List<String> {
    return file.readText()
            .split("{end}\n\n").dropLast(1)
            .map { if (it.startsWith("\uFEFF")) it.substring(1) else it }
            .map { if (stripNewLine) it.replace("\n", "") else it }
}
