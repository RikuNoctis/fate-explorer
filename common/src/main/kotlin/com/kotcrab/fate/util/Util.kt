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

package com.kotcrab.fate.util

import kio.KioInputStream
import kio.KioOutputStream
import kio.LERandomAccessFile
import kio.util.WINDOWS_932
import kio.util.appendLine
import java.nio.charset.Charset


/** @author Kotcrab */

fun StringBuilder.appendWindowsLine(text: String = "") {
    appendLine(text, "\r\n")
}

fun KioInputStream.readDatString(
    at: Int = pos(), maintainStreamPos: Boolean = false, fixUpNewLine: Boolean = false,
    charset: Charset = Charsets.WINDOWS_932
): String {
    val prevPos = longPos()
    setPos(at)
    var charCount = 0
    while (true) {
        charCount++
        val character = readByte().toChar()
        if (character.toInt() == 0x00 && pos().rem(4) == 0) {
            break
        }
    }
    setPos(at)
    val bytes = ByteArray(charCount)
    var idx = 0
    while (true) {
        val character = readByte()
        if (character != 0x00.toByte()) {
            bytes[idx++] = character
        } else if (pos().rem(4) == 0) {
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

fun KioOutputStream.writeDatString(string: String, charset: Charset = Charsets.WINDOWS_932): Int {
    val bytes = string.toByteArray(charset)
    writeBytes(bytes)
    val padding = 4 - (bytes.size % 4)
    writeBytes(ByteArray(padding))
    return bytes.size + padding
}

fun LERandomAccessFile.writeDatString(string: String, charset: Charset = Charsets.WINDOWS_932): Int {
    val bytes = string.toByteArray(charset)
    write(bytes)
    val padding = 4 - (bytes.size % 4)
    write(ByteArray(padding))
    return bytes.size + padding
}
