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

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.util.getBits
import com.kotcrab.fate.util.toUnsignedInt
import java.io.File

/**
 * .CMP (IECP) file decompressor
 * @author Kotcrab
 * */
class CmpFile(file: File) {
    private lateinit var data: DecodedData

    init {
        with(FateInputStream(file)) {
            if (readString(4) != "IECP") error("Not a CMP file.")

            val decompressedSize = readInt()
            data = DecodedData(decompressedSize)
            val dict = Dictionary()

            while (true) {
                if (eof()) break
                val controlByte = readByte()
                controlByte.getBits().forEach { clearByte ->
                    if (data.eof()) return@forEach
                    if (clearByte) {
                        val byte = readByte()
                        data.write(byte)
                        dict.write(byte)
                    } else {
                        val b1 = readByte().toUnsignedInt()
                        val b2 = readByte().toUnsignedInt()

                        //because it doesn't make sense to compress anything smaller than two bytes,
                        //there are subtracted from length in order to allow compressing more characters
                        val length = (b2 and 0xF) + 2
                        val offset = ((b2 and 0xF0) shl 4) or b1

                        for (localOffset in 0..length) {
                            val dictOffset = (offset + localOffset) and 0xFFF
                            val byte = dict[dictOffset]

                            data.write(byte)
                            dict.write(byte)
                        }
                    }
                }
                if (data.eof()) break
            }
            close()
        }
    }

    fun getData(): ByteArray {
        return data.data
    }

    fun writeToFile(path: String) {
        data.writeToFile(path)
    }
}

private class Dictionary {
    val data = ByteArray(4096)
    var pointer = 0xFEE

    fun write(byte: Byte) {
        data[pointer] = byte
        pointer++
        if (pointer == data.size) {
            pointer = 0
        }
    }

    operator fun get(offset: Int): Byte {
        return data[offset]
    }
}

private class DecodedData(size: Int) {
    val data = ByteArray(size)
    var pointer = 0

    fun write(byte: Byte) {
        data[pointer] = byte
        pointer++
    }

    fun eof(): Boolean {
        return pointer == data.size
    }

    fun writeToFile(path: String) {
        val out = File(path)
        out.createNewFile()
        out.writeBytes(data)
    }
}
