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

import com.kotcrab.fate.file.extra.ExtraItemParam01Entry
import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.io.FateOutputStream
import com.kotcrab.fate.patcher.extra.ExtraTranslation
import com.kotcrab.fate.util.WINDOWS_932
import java.io.File
import java.nio.charset.Charset

/**
 * @author Kotcrab
 * */
class ExtraItemParam01BinFilePatcher(origBytes: ByteArray, outFile: File, translation: ExtraTranslation, translationOffset: Int,
                                     charset: Charset = Charsets.WINDOWS_932) {

    init {
        val out = FateOutputStream(outFile)
        var entryCount = 0

        with(FateInputStream(origBytes)) {
            val entries = readInt()
            val u1 = readInt()
            val u2 = readInt()
            val u3 = readInt()
            out.writeInt(entries)
            out.writeInt(u1)
            out.writeInt(u2)
            out.writeInt(u3)

            repeat(entries) {
                val origEntryBytes = readBytes(0x100)
                var origEntry: ExtraItemParam01Entry? = null
                with(FateInputStream(origEntryBytes)) itemParse@{
                    val name = readDatString(maintainStreamPos = true, charset = charset)
                    skip(0x54)
                    val buyValue = readInt()
                    val sellValue = readInt()
                    val desc = readDatString(maintainStreamPos = true, charset = charset)
                    skip(0x34)
                    val trivia1 = readDatString(maintainStreamPos = true, charset = charset)
                    skip(0x34)
                    val trivia2 = readDatString(maintainStreamPos = true, charset = charset)
                    skip(0x34)
                    origEntry = ExtraItemParam01Entry(origEntryBytes, name, buyValue, sellValue, desc, trivia1, trivia2)
                }
                if (origEntry!!.isUnused()) {
                    out.writeBytes(origEntryBytes)
                } else {
                    with(FateInputStream(origEntryBytes)) {
                        run {
                            val newText = translation.getTranslation(entryCount, translationOffset)
                            entryCount++
                            if (newText.length > 0x17) error("Max length for item name exceeded: $newText")
                            out.writeString(newText, 0x18)
                            skip(0x18)
                        }
                        out.writeBytes(readBytes(0x44))
                        repeat(3) {
                            val newText = translation.getTranslation(entryCount, translationOffset)
                            entryCount++
                            if (newText.length > 0x33) error("Max length for item name exceeded: $newText")
                            out.writeString(newText, 0x34)
                            skip(0x34)
                        }
                        out.writeBytes(readBytes(0x8))
                        skip(0x8)
                    }
                }
            }
            close()
        }

        out.align(16)
        out.close()
    }
}
