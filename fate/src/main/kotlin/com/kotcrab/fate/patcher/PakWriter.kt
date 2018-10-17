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

import com.kotcrab.fate.file.PakFileEntry
import kio.KioOutputStream
import java.io.File

/**
 * Writes new .PAK file from specified entries. Only mode 0x8000 is supported (file paths included in output .pak).
 */
class PakWriter(val entries: List<PakFileEntry>, val outFile: File) {
    init {
        with(KioOutputStream(outFile)) {
            writeShort(entries.size.toShort())
            writeShort(0x8000.toShort()) //paths included

            entries.forEach {
                writeInt(it.bytes.size)
            }

            align(16)

            entries.forEach { entry ->
                writeString(entry.path, 64)
                writeBytes(entry.bytes)
                align(16)
            }
            close()
        }
    }
}
