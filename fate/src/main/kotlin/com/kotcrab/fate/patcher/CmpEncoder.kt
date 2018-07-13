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

package com.kotcrab.fate.patcher

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.io.FateOutputStream
import com.kotcrab.fate.util.setBit
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * .CMP (IECP) file encoder.
 * @author Kotcrab
 * */
class CmpEncoder(file: File, secondPass: Boolean) {
    private val outStream = ByteArrayOutputStream()

    companion object {
        private const val MAX_CODED_PAIR_SIZE = 18
    }

    init {
        val dict = Dictionary(secondPass)
        var flag: Byte = 0
        var flagPos = 0
        var buffer = FateOutputStream(ByteArrayOutputStream())
        var dataBuffer = FateOutputStream(ByteArrayOutputStream())

        val out = FateOutputStream(outStream)
        out.writeString("IECP", 4)
        out.writeInt(file.length().toInt())

        fun writeDataBuffer() {
            out.writeByte(flag)
            out.writeBytes(dataBuffer.getAsByteArrayOutputStream().toByteArray())
            flag = 0
            flagPos = 0
            dataBuffer = FateOutputStream(ByteArrayOutputStream())
        }

        fun writePair(pairBytes: ByteArray, setFlagBit: Boolean) {
            dataBuffer.writeBytes(pairBytes)
            if (setFlagBit) {
                flag = flag.setBit(flagPos)
            }
            flagPos++
            if (flagPos == 8) {
                writeDataBuffer()
            }
        }

        with(FateInputStream(file)) {
            while (!eof()) {
                val byte = readByte()
                buffer.writeByte(byte)
                val bufferBytes = buffer.getAsByteArrayOutputStream().toByteArray()
                val index = dict.getSubArrayIndex(bufferBytes)
                if (index == -1 || bufferBytes.size == MAX_CODED_PAIR_SIZE || eof()) {
                    if (bufferBytes.size <= 3) {
                        //write uncoded bytes
                        bufferBytes.forEach { bufByte ->
                            writePair(byteArrayOf(bufByte), true)
                            dict.write(bufByte)
                        }
                        buffer = FateOutputStream(ByteArrayOutputStream())
                    } else {
                        //write coded pair
                        val bufToWrite = bufferBytes.sliceArray(0 until bufferBytes.lastIndex)
                        val bufToWriteIndex = dict.getSubArrayIndex(bufToWrite)
                        val codedPair = ((bufToWriteIndex and 0xFF shl 8) or (bufToWriteIndex and 0xF00 ushr 8 shl 4)
                                or (bufToWrite.size - 3 and 0xF)) and 0xFFFF
                        val c1 = (codedPair ushr 8 and 0xFF).toByte()
                        val c2 = (codedPair and 0xFF).toByte()
                        writePair(byteArrayOf(c1, c2), false)
                        bufToWrite.forEach { bufByte -> dict.write(bufByte) }
                        setPos(count() - 1)
                        buffer = FateOutputStream(ByteArrayOutputStream())
                    }
                }
            }
            writeDataBuffer()
        }
    }

    fun getData() = outStream.toByteArray()

    private class Dictionary(private val secondPass: Boolean) {
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

        fun getSubArrayIndex(needle: ByteArray): Int {
            outer@ for (i in 0..pointer - needle.size) {
                for (j in needle.indices) {
                    if (data[i + j] != needle[j]) {
                        continue@outer
                    }
                }
                return i
            }
            if (secondPass) {
                outer@ for (i in pointer..data.size - needle.size) {
                    for (j in needle.indices) {
                        if (data[i + j] != needle[j]) {
                            continue@outer
                        }
                    }
                    return i
                }
            }
            return -1
        }
    }
}
