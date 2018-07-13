/*
 * Copyright 2017-2018 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
