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

import kio.KioInputStream
import kio.util.WINDOWS_932
import java.io.EOFException
import java.io.File

/** @author Kotcrab */
class SjisFile(bytes: ByteArray) {
    constructor(file: File) : this(file.readBytes())

    val entries = mutableListOf<SjisEntry>()

    init {
        with(KioInputStream(bytes)) {
            while (!eof()) {
                try {
                    val l1 = readNullTerminatedString(Charsets.WINDOWS_932)
                    val l2 = readNullTerminatedString(Charsets.WINDOWS_932)
                    val l3 = readNullTerminatedString(Charsets.WINDOWS_932)
                    val l4 = readNullTerminatedString(Charsets.WINDOWS_932)
                    val l5 = readNullTerminatedString(Charsets.WINDOWS_932)
                    if (arrayOf(l1, l2, l3, l4, l5).all { it.isNotBlank() }) {
                        entries.add(SjisEntry(l1, l2, l3, l4, l5))
                    }
                } catch (ignored: EOFException) {
                    // assuming file can have some padding
                }
            }
            close()
        }
    }
}

class SjisEntry(val l1: String, val l2: String, val l3: String, val l4: String, val l5: String) {
    fun isUnused(): Boolean =
        l1 in listOf("（予備）", "0") && l2 in listOf("（予備）", "0") && l3 == "0" && l4 == "0" && l5 == "0"
}
