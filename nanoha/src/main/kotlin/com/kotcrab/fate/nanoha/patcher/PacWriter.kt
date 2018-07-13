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

import com.kotcrab.fate.io.FateOutputStream
import com.kotcrab.fate.nanoha.file.PacFileEntry
import java.io.ByteArrayOutputStream
import java.io.File

/** @author Kotcrab */
class PacWriter(private val pacEntries: List<PacFileEntry>) {
    fun writeToFile(out: File) {
        val entries = pacEntries.map { EntryIntermediate(it, -1, -1) }

        val nameBytes = with(FateOutputStream(ByteArrayOutputStream())) {
            entries.forEach {
                it.nameLocalPtr = count()
                writeDatString(it.entry.fileName)
            }
            align(0x10)
            close()
            getAsByteArrayOutputStream().toByteArray()
        }


        val dataBytes = with(FateOutputStream(ByteArrayOutputStream())) {
            entries.forEach {
                it.dataLocalPtr = count()
                writeBytes(it.entry.bytes)
                align(0x10)
            }
            close()
            getAsByteArrayOutputStream().toByteArray()
        }

        val bs = ByteArrayOutputStream()
        with(FateOutputStream(bs)) {
            val headerSize = 0x20 + 0x20 * pacEntries.size
            writeString("add", 4)
            writeInt(4) //version
            writeInt(0x20) //offset
            writeInt(0x20) //offset
            writeInt(pacEntries.size)
            writeInt(headerSize + nameBytes.size + dataBytes.size) //file size
            writeInt(0) //dummy
            writeInt(0) //dummy

            entries.forEach {
                writeInt(headerSize + nameBytes.size + it.dataLocalPtr) //file data ptr
                writeInt(it.entry.fileSize)
                writeInt(0) //dummy
                writeInt(it.entry.timet)
                writeInt(headerSize + it.nameLocalPtr) //file name ptr
                writeInt(0) //dummy
                writeInt(0) //dummy
                writeInt(0) //dummy
            }

            writeBytes(nameBytes)
            writeBytes(dataBytes)

            close()
        }
        out.writeBytes(bs.toByteArray())
    }
}

private class EntryIntermediate(val entry: PacFileEntry, var nameLocalPtr: Int, var dataLocalPtr: Int)
