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

package com.kotcrab.fate.nanoha.file

import com.kotcrab.fate.io.FateInputStream
import java.io.File

/** @author Kotcrab */
@Suppress("UNUSED_VARIABLE")
class PacFile(bytes: ByteArray) {
    constructor(file: File) : this(file.readBytes())

    val entries: Array<PacFileEntry>

    init {
        val fileEntries = mutableListOf<PacFileEntry>()

        with(FateInputStream(bytes)) {
            if (readStringAndTrim(4) != "add") error("Not a PAC 'add' file")
            val version = readInt()
            if (version != 4) error("Only pac file version 4 is supported")
            val offset = readInt() // 0x20
            val headerSize = readInt() // 0x20
            val files = readInt()
            val totalFileSize = readInt()
            skip(0x8)

            repeat(files) {
                val fileDataPtr = readInt()
                val fileSize = readInt()
                skip(0x4)
                val timet = readInt()
                val fileNamePtr = readInt()
                skip(0xC)

                temporaryJump(fileNamePtr) {
                    val fileName = readNullTerminatedString()
                    temporaryJump(fileDataPtr) {
                        val fileBytes = readBytes(fileSize)
                        fileEntries.add(PacFileEntry(fileName, fileSize, timet, fileBytes))
                    }
                }
            }

            close()
        }

        entries = fileEntries.toTypedArray()
    }
}

class PacFileEntry(val fileName: String, val fileSize: Int, val timet: Int, val bytes: ByteArray)
