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

package com.kotcrab.fate.file

import com.kotcrab.fate.util.readDatString
import kio.KioInputStream
import kio.util.WINDOWS_932
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class IndexedTextBinFile(file: File, charset: Charset = Charsets.WINDOWS_932) {
    val entries = mutableListOf<Pair<Int, String>>()

    init {
        with(KioInputStream(file)) {
            var lastIndex = 0
            while (true) {
                val index = readInt()
                if (index < lastIndex) break
                val length = readInt()
                if (length == 0) break
                val text = readDatString(fixUpNewLine = true, charset = charset)
                entries.add(index to text)
                lastIndex++
                if (eof()) break
            }
            close()
        }
    }
}
