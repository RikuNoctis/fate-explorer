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

package com.kotcrab.fate.patcher.extra.file

import com.kotcrab.fate.patcher.extra.ExtraTranslation
import com.kotcrab.fate.util.readDatString
import com.kotcrab.fate.util.writeDatString
import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraIndexedTextBinFilePatcher(
    origBytes: ByteArray, outFile: File, translation: ExtraTranslation, translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932
) {
    init {
        val out = KioOutputStream(outFile)
        with(KioInputStream(origBytes)) {
            var entryCount = 0
            while (true) {
                val index = readInt()
                if ((index == 0 && entryCount > 0) || eof()) break
                val len = readInt()
                val text = readDatString(fixUpNewLine = true, charset = charset)
                val newText = translation.getTranslation(entryCount, translationOffset)
                entryCount++
                if (newText.isBlank()) {
                    out.writeInt(index)
                    out.writeInt(len)
                    out.writeDatString(text, charset = charset)
                } else {
                    out.writeInt(index)
                    val bytes = newText.toByteArray(charset)
                    out.writeInt(bytes.size + (4 - (bytes.size % 4)))
                    out.writeDatString(newText, charset = charset)
                }
                if (eof()) break
            }
            close()
        }
        out.align(16)
        out.close()
    }
}
