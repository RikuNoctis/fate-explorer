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

import com.kotcrab.fate.util.readDatString
import kio.KioInputStream
import java.io.File

/**
 * @author Kotcrab
 */
class ExtraItemParam01BinFile(bytes: ByteArray, jpSize: Boolean) {
    constructor(file: File, jpSize: Boolean) : this(file.readBytes(), jpSize)

    val entries: Array<ExtraItemParam01Entry>

    init {
        val itemEntries = mutableListOf<ExtraItemParam01Entry>()

        with(KioInputStream(bytes)) {
            val entries = readInt()
            readInt()
            readInt()
            readInt()
            repeat(entries) {
                val entryBytes = readBytes(if (jpSize) 0xE4 else 0x100)
                with(KioInputStream(entryBytes)) itemParse@{
                    val name = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x54 else 0x54)
                    val buyValue = readInt()
                    val sellValue = readInt()
                    val desc = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x30 else 0x34)
                    val trivia1 = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x30 else 0x34)
                    val trivia2 = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x28 else 0x34)
                    itemEntries.add(
                        ExtraItemParam01Entry(entryBytes, name, buyValue, sellValue, desc, trivia1, trivia2)
                    )
                }
            }
            close()
        }

        entries = itemEntries.toTypedArray()
    }
}

class ExtraItemParam01Entry(
    val bytes: ByteArray, val name: String, val buyValue: Int, val sellValue: Int,
    val description: String, val trivia1: String, val trivia2: String
) {
    fun isUnused(): Boolean = name == "（予備）"
}
