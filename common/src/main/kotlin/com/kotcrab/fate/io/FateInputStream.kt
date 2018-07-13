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

package com.kotcrab.fate.io

import com.google.common.io.CountingInputStream
import com.google.common.io.LittleEndianDataInputStream
import com.kotcrab.fate.util.WINDOWS_932
import com.kotcrab.fate.util.toHex
import com.kotcrab.fate.util.toUnsignedInt
import java.io.*
import java.nio.charset.Charset

/** @author Kotcrab */
class FateInputStream(baseStream: InputStream, val size: Long, littleEndian: Boolean = true) {
    constructor(file: File, littleEndian: Boolean = true)
            : this(MarkableFileInputStream(FileInputStream(file)), file.length(), littleEndian)

    constructor(bytes: ByteArray, littleEndian: Boolean = true)
            : this(ByteArrayInputStream(bytes), bytes.size.toLong(), littleEndian)

    private val counter = CountingInputStream(baseStream)
    private val stream: FilterInputStream = if (littleEndian) LittleEndianDataInputStream(counter) else DataInputStream(counter)
    private val input: DataInput = stream as DataInput

    init {
        stream.mark(0)
    }

    fun readShort(): Short {
        return input.readShort()
    }

    fun readShort(at: Int): Short {
        val prevPos = longCount()
        setPos(at)
        val value = readShort()
        setPos(prevPos)
        return value
    }

    fun readInt(): Int {
        return input.readInt()
    }

    fun readInt(at: Int): Int {
        val prevPos = longCount()
        setPos(at)
        val value = readInt()
        setPos(prevPos)
        return value
    }

    fun readLong(): Long {
        return input.readLong()
    }

    fun readLong(at: Int): Long {
        val prevPos = longCount()
        setPos(at)
        val value = readLong()
        setPos(prevPos)
        return value
    }

    fun readFloat(): Float {
        return input.readFloat()
    }

    fun readFloat(at: Int): Float {
        val prevPos = longCount()
        setPos(at)
        val value = readFloat()
        setPos(prevPos)
        return value
    }

    fun readString(length: Int, charset: Charset = Charsets.UTF_8): String {
        return String(readBytes(length), charset)
    }

    fun readStringAndTrim(length: Int, charset: Charset = Charsets.UTF_8): String {
        return readString(length, charset).replace("\u0000", "")
    }

    fun readNullTerminatedString(charset: Charset = Charsets.US_ASCII): String {
        val out = ByteArrayOutputStream()
        while (true) {
            val byte = readByte().toInt()
            if (byte == 0) break
            out.write(byte)
        }
        return String(out.toByteArray(), charset)
    }

    fun readDoubleNullTerminatedString(charset: Charset): String {
        val out = ByteArrayOutputStream()
        while (true) {
            val byte1 = readByte().toInt()
            val byte2 = readByte().toInt()
            if (byte1 == 0 && byte2 == 0) break
            out.write(byte1)
            out.write(byte2)
        }
        return String(out.toByteArray(), charset)
    }

    fun readDatString(at: Int = count(), maintainStreamPos: Boolean = false, fixUpNewLine: Boolean = false,
                      charset: Charset = Charsets.WINDOWS_932): String {
        val prevPos = longCount()
        setPos(at)
        var charCount = 0
        while (true) {
            charCount++
            val character = input.readByte().toChar()
            if (character.toInt() == 0x00 && count().rem(4) == 0) {
                break
            }
        }
        setPos(at)
        val bytes = ByteArray(charCount)
        var idx = 0
        while (true) {
            val character = input.readByte()
            if (character != 0x00.toByte()) {
                bytes[idx++] = character
            } else if (count().rem(4) == 0) {
                break
            }
        }
        if (maintainStreamPos) {
            setPos(prevPos)
        }
        val result = String(bytes, charset).replace("\u0000", "")
        if (fixUpNewLine) {
            return result.replace("\n\r", "\r\n")
        }
        return result
    }

    fun readBytesAsHexString(count: Int): String {
        return readBytes(count).joinToString(separator = "", transform = { it.toHex() })
    }

    fun readBytes(byteArray: ByteArray): ByteArray {
        stream.read(byteArray)
        return byteArray
    }

    fun readBytes(length: Int): ByteArray {
        return readBytes(ByteArray(length))
    }

    fun readByte(): Byte {
        return input.readByte()
    }

    fun readByte(at: Int): Byte {
        val prevPos = longCount()
        setPos(at)
        val value = readByte()
        setPos(prevPos)
        return value
    }

    fun align(pad: Long) {
        if (longCount() % pad == 0L) return
        val absOffset = (longCount() / pad + 1) * pad
        setPos(absOffset)
    }

    fun temporaryJump(addr: Int, reader: (FateInputStream) -> Unit) {
        val lastPos = count()
        setPos(addr)
        reader(this)
        setPos(lastPos)
    }

    fun skip(n: Int) {
        input.skipBytes(n)
    }

    fun skipNullBytes() {
        skipByte(0)
    }

    fun skipByte(byteToSkip: Int) {
        while (true) {
            if (eof()) return
            val byte = readByte().toUnsignedInt()
            if (byte != byteToSkip) {
                setPos(longCount() - 1L)
                return
            }
        }
    }

    fun longCount(): Long {
        return counter.count
    }

    fun count(): Int {
        return longCount().toInt()
    }

    fun eof(): Boolean {
        return longCount() == size
    }

    fun close() {
        stream.close()
    }

    fun setPos(pos: Int) {
        setPos(pos.toLong())
    }

    fun setPos(pos: Long) {
        counter.reset()
        stream.reset()
        stream.skip(pos)
    }
}
