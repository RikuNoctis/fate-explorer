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

import com.kotcrab.fate.file.extra.ExtraItemParam04Entry
import com.kotcrab.fate.patcher.extra.ExtraTranslation
import com.kotcrab.fate.util.readDatString
import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import kio.util.toWHex
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraItemParam04BinFilePatcher(origBytes: ByteArray, outFile: File, translation: ExtraTranslation, translationOffset: Int,
                                     charset: Charset = Charsets.WINDOWS_932) {

    init {
        val out = KioOutputStream(outFile)
        var entryCount = 0

        with(KioInputStream(origBytes)) {
            val entries = readInt()
            val u1 = readInt()
            val u2 = readInt()
            val u3 = readInt()
            out.writeInt(entries)
            out.writeInt(u1)
            out.writeInt(u2)
            out.writeInt(u3)

            repeat(entries) { _ ->
                val origEntryBytes = readBytes(0xC0)
                var origEntry: ExtraItemParam04Entry? = null
                with(KioInputStream(origEntryBytes)) itemParse@{
                    val name = readDatString(maintainStreamPos = true, charset = charset)
                    skip(0x48)
                    val buyValue = readInt()
                    val sellValue = readInt()
                    val desc = readDatString(maintainStreamPos = true, charset = charset)
                    skip(0x34)
                    val trivia = readDatString(maintainStreamPos = true, charset = charset)
                    skip(0x34)
                    origEntry = ExtraItemParam04Entry(origEntryBytes, name, buyValue, sellValue, desc, trivia)
                }
                if (origEntry!!.isUnused()) {
                    out.writeBytes(origEntryBytes)
                } else {
                    with(KioInputStream(origEntryBytes)) {
                        run {
                            val newText = translation.getTranslation(entryCount, translationOffset)
                            entryCount++
                            if (newText.length > 0x17) error("Max length for item name exceeded: $newText")
                            out.writeString(newText, 0x18)
                            skip(0x18)
                        }
                        out.writeBytes(readBytes(0x38))
                        run {
                            val newText = translation.getTranslation(entryCount, translationOffset)
                            entryCount++
                            if (newText.length > 0x33) error("Max length for item name exceeded: $newText")
                            out.writeString(newText, 0x34)
                            skip(0x34)
                        }
                        run {
                            val newText = translation.getTranslation(entryCount, translationOffset)
                            entryCount++
                            if (newText.length > 0x33) error("Max length for item name exceeded: $newText at ${out.pos().toWHex()}")
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
