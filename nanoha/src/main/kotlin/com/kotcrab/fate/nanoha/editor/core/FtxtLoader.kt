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

package com.kotcrab.fate.nanoha.editor.core

import com.kotcrab.fate.util.walkDir
import java.io.File
import java.util.*

/** @author Kotcrab */
class FtxtLoader {
    val entries = TreeMap<File, Array<IntArray>>()

    fun load(asmDir: File) {
        walkDir(asmDir, processFile = { asmFile ->
            val ftxt = FtxtDecoder(asmFile)
            entries[asmFile] = ftxt.texts
        })
    }

    fun getCount(): Int {
        return entries.values.sumBy { it.size }
    }

    fun getEntry(file: File, ftxtIndex: Int): IntArray? {
        val texts = entries[file] ?: return null
        return texts[ftxtIndex]
    }
}
