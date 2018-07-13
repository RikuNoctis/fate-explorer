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

/**
 * @author Kotcrab
 */
class ExtraItemParam04BinFile(bytes: ByteArray, jpSize: Boolean) {
    constructor(file: File, jpSize: Boolean) : this(file.readBytes(), jpSize)

    val entries: Array<ExtraItemParam04Entry>

    init {
        val itemEntries = mutableListOf<ExtraItemParam04Entry>()

        with(FateInputStream(bytes)) {
            val entries = readInt()
            readInt()
            readInt()
            readInt()
            repeat(entries) { _ ->
                val entryBytes = readBytes(if (jpSize) 0xB8 else 0xC0)
                with(FateInputStream(entryBytes)) itemParse@{
                    val name = readDatString(maintainStreamPos = true)
                    skip(0x48)
                    val buyValue = readInt()
                    val sellValue = readInt()
                    val desc = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x30 else 0x34)
                    val trivia = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x28 else 0x34)
                    itemEntries.add(ExtraItemParam04Entry(entryBytes, name, buyValue, sellValue, desc, trivia))
                }
            }
            close()
        }

        entries = itemEntries.toTypedArray()
    }
}

class ExtraItemParam04Entry(val bytes: ByteArray, val name: String, val buyValue: Int, val sellValue: Int,
                            val description: String, val trivia: String) {
    fun isUnused(): Boolean = name in listOf("（予備）", "-")
}
