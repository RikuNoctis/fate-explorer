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

package com.kotcrab.fate.nanoha.patcher

import com.kotcrab.fate.util.writeDatString
import kio.KioOutputStream
import kio.util.padArray
import java.io.ByteArrayOutputStream
import java.io.File

/** @author Kotcrab */
class LzsEncoder(inFile: File, lzsName: String) {
    var compressedBytes: ByteArray

    init {
        val lzsNameLen = with(KioOutputStream(ByteArrayOutputStream())) {
            writeDatString(lzsName)
            getAsByteArrayOutputStream().toByteArray().size
        }

        val bs = ByteArrayOutputStream()
        with(KioOutputStream(bs)) {
            val comprOffset = 0x20 + lzsNameLen
            val inBytes = padArray(inFile.readBytes())
            val compr = ByteArray(inBytes.size / 8, { 0xFF.toByte() })
            val dictOffset = comprOffset + compr.size
            val dict = inBytes

            writeString("LZS", 4)
            writeInt(0x80505) //magic
            writeInt(comprOffset) //comr offset
            writeInt(dictOffset) //dict offset

            writeInt(inBytes.size) //decompressed size
            writeInt(dictOffset + dict.size)//lzs file size
            writeInt(0x0200) //magic
            writeInt(0) //dummy

            writeDatString(lzsName)
            writeBytes(compr) //compr
            writeBytes(dict) //dict
        }
        compressedBytes = bs.toByteArray()
    }
}
