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

class SequentialArrayReader(public val bytes: ByteArray) {
    public var pos = 0
        private set
    val size = bytes.size

    fun read(): Byte {
        val byte = bytes[pos]
        pos++
        return byte
    }

    fun read(n: Int): ByteArray {
        val outBytes = ByteArray(n)
        repeat(n) {
            outBytes[it] = read()
        }
        return outBytes
    }
}

class SequentialArrayWriter(public val bytes: ByteArray) {
    public var pos = 0
        private set
    val size = bytes.size

    fun write(byte: Byte) {
        bytes[pos] = byte
        pos++
    }

    fun write(inBytes: ByteArray) {
        inBytes.forEach { write(it) }
    }

    operator fun get(idx: Int): Byte {
        return bytes[idx]
    }
}
