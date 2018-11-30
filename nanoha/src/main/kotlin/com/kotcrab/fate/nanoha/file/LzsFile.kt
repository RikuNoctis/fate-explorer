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

import kio.KioInputStream
import kio.SequentialArrayReader
import kio.SequentialArrayWriter
import kio.util.toUnsignedInt
import java.io.File

/** @author Kotcrab */
class LzsFile(bytes: ByteArray) {
    constructor(file: File) : this(file.readBytes())

    val decompressedBytes: ByteArray
    val fileName: String

    init {
        with(KioInputStream(bytes)) {
            if (readStringAndTrim(4) != "LZS") error("Not a LZS file")
            readInt() //always 0x80505
            val offset = readInt()
            val dictOffset = readInt()
            val decompressedSize = readInt()
            val fileSize = readInt()
            readInt() //always 0x0200
            skip(4)
            fileName = readNullTerminatedString(Charsets.US_ASCII)

            setPos(dictOffset)
            val dictSize = fileSize - dictOffset
            val dict = readBytes(dictSize)

            setPos(offset)
            val comprSize = dictOffset - offset
            val compr = readBytes(comprSize)

            decompressedBytes = ByteArray(decompressedSize)
            decompress(
                SequentialArrayReader(dict),
                SequentialArrayReader(compr),
                SequentialArrayWriter(decompressedBytes)
            )

            close()
        }
    }

    private fun decompress(
        dictReader: SequentialArrayReader,
        comprReader: SequentialArrayReader,
        decompWriter: SequentialArrayWriter
    ) {
        val bits = 5
        val threshold = 2

        var flag: Int
        while (comprReader.pos < comprReader.size) {
            flag = comprReader.read().toUnsignedInt() or (comprReader.read().toUnsignedInt() shl 8)

            repeat(16) { f ->
                if (flag ushr f and 1 != 0) {
                    if (dictReader.pos == dictReader.size) return
                    decompWriter.write(dictReader.read())
                } else {
                    if (comprReader.pos == comprReader.size) return
                    val t = comprReader.read().toUnsignedInt() or (comprReader.read().toUnsignedInt() shl 8)
                    val size = (t and ((1 shl bits) - 1)) + threshold
                    val offset = (t ushr bits)

                    repeat(size) {
                        decompWriter.write(decompWriter[decompWriter.pos - offset])
                    }
                }
            }
        }
    }

}
