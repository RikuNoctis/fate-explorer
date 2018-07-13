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

package com.kotcrab.fate.file.extra

import com.kotcrab.fate.io.FateInputStream
import java.io.File

/** @author Kotcrab */
class ExtraInfoMatrixBinFile(dFile: File, tFile: File) {
    val entries = mutableListOf<ExtraInfoMatrixEntry>()

    init {
        val titles = mutableListOf<String>()
        val texts = mutableListOf<String>()
        val shortcuts = mutableListOf<String>()

        with(FateInputStream(dFile)) {
            val id = readInt()
            skip(0x50)
            repeat(3) {
                val text = readDatString(maintainStreamPos = true)
                titles.add(text)
                skip(0x20)
            }
            repeat(6) {
                val text = readDatString(maintainStreamPos = true)
                titles.add(text)
                skip(0x80)
            }
            close()
        }
        with(FateInputStream(tFile)) {
            val id = readInt()
            repeat(9) { _ ->
                val text = readDatString(maintainStreamPos = true, fixUpNewLine = true)
                texts.add(text)
                skip(0x1400)
            }
            repeat(9) { _ ->
                val text = readDatString(maintainStreamPos = true)
                shortcuts.add(text)
                skip(0x44)
            }
            close()
        }

        if (titles.size != texts.size || texts.size != shortcuts.size) error("infomatrix content count mismatch")
        repeat(titles.count()) { idx ->
            entries.add(ExtraInfoMatrixEntry(titles[idx], texts[idx], shortcuts[idx]))
        }
    }
}

class ExtraInfoMatrixEntry(val title: String, val text: String, val shortcut: String)
