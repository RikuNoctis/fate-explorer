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
import com.kotcrab.fate.util.writeDatString
import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraInfoMatrixBinDFilePatcher(
    origBytes: ByteArray, outFile: File, translation: ExtraTranslation, translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932
) {
    init {
        val out = KioOutputStream(outFile)
        var entryCount = 0
        with(KioInputStream(origBytes)) {
            out.writeBytes(readBytes(0x54))
            repeat(3) {
                val len = out.writeDatString(
                    translation.getTranslation(entryCount, translationOffset),
                    charset = charset
                )
                if (len > 0x20) error("Too long translation text")
                out.writeNullBytes(0x20 - len)
                entryCount++
                entryCount++
                entryCount++
                skip(0x20)
            }
            repeat(6) {
                val len = out.writeDatString(
                    translation.getTranslation(entryCount, translationOffset),
                    charset = charset
                )
                if (len > 0x80) error("Too long translation text")
                out.writeNullBytes(0x80 - len)
                entryCount++
                entryCount++
                entryCount++
                skip(0x80)
            }
            out.writeBytes(readBytes((size - longPos()).toInt()))
            close()
        }
        out.align(16)
        out.close()
    }
}
