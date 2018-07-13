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

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.io.FateOutputStream
import com.kotcrab.fate.patcher.extra.ExtraTranslation
import com.kotcrab.fate.util.WINDOWS_932
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraInfoMatrixBinTFilePatcher(origBytes: ByteArray, outFile: File, translation: ExtraTranslation, translationOffset: Int,
                                     charset: Charset = Charsets.WINDOWS_932) {
    init {
        val out = FateOutputStream(outFile)
        var entryCount = 0
        with(FateInputStream(origBytes)) {
            out.writeInt(readInt())
            repeat(9) {
                entryCount++
                entryCount++
                val len = out.writeDatString(translation.getTranslation(entryCount, translationOffset),
                        charset = charset)
                if (len > 0x1400) error("Too long translation text")
                out.writeBytes(0x1400 - len)
                entryCount++
            }
            entryCount = 0
            repeat(9) {
                entryCount++
                val len = out.writeDatString(translation.getTranslation(entryCount, translationOffset),
                        charset = charset)
                if (len > 0x44) error("Too long translation text")
                out.writeBytes(0x44 - len)
                entryCount++
                entryCount++
            }

            entryCount = 0

            close()
        }
        out.align(16)
        out.close()
    }
}
