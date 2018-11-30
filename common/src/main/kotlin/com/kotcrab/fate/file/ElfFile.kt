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
import kio.util.toUnsignedInt
import kio.util.toWHex
import java.io.File

/** @author Kotcrab */
class ElfFile(val bytes: ByteArray) {
    constructor(file: File) : this(file.readBytes())

    val header: ElfHeader
    val programHeaders: Array<ElfProgramHeader>
    val sectionHeaders: Array<ElfSectionHeader>

    init {
        val programHeaders = mutableListOf<ElfProgramHeader>()
        val sectionHeaders = mutableListOf<ElfSectionHeader>()

        with(KioInputStream(bytes)) {

            if (readByte().toUnsignedInt() != 0x7F || readString(3) != "ELF") error("Not an ELF file")
            header = ElfHeader(
                entryPoint = readInt(at = 0x18),
                programHeaderOffset = readInt(at = 0x1C),
                programHeaderSize = readShort(at = 0x2A).toInt(),
                programHeaderCount = readShort(at = 0x2C).toInt(),
                sectionHeaderOffset = readInt(at = 0x20),
                sectionHeaderSize = readShort(at = 0x2E).toInt(),
                sectionHeaderCount = readShort(at = 0x30).toInt()
            )

            setPos(header.programHeaderOffset)
            repeat(header.programHeaderCount) {
                val progBytes = readBytes(header.programHeaderSize)
                with(KioInputStream(progBytes)) {
                    programHeaders.add(
                        ElfProgramHeader(
                            offset = readInt(at = 0x04),
                            vAddr = readInt(at = 0x08),
                            memSize = readInt(at = 0x14)
                        )
                    )
                }
            }

            setPos(header.sectionHeaderOffset)
            repeat(header.sectionHeaderCount) {
                val sectBytes = readBytes(header.sectionHeaderSize)
                with(KioInputStream(sectBytes)) {
                    sectionHeaders.add(
                        ElfSectionHeader(
                            type = readInt(at = 0x04),
                            vAddr = readInt(at = 0x0C),
                            offset = readInt(at = 0x10),
                            size = readInt(at = 0x14)
                        )
                    )
                }
            }
        }

        this.programHeaders = programHeaders.toTypedArray()
        this.sectionHeaders = sectionHeaders.toTypedArray()
    }
}

class ElfHeader(
    val entryPoint: Int,
    val programHeaderOffset: Int, val programHeaderSize: Int, val programHeaderCount: Int,
    val sectionHeaderOffset: Int, val sectionHeaderSize: Int, val sectionHeaderCount: Int
) {
    override fun toString(): String {
        return "ElfHeader(entryPoint=${entryPoint.toWHex()}, programHeaderOffset=${programHeaderOffset.toWHex()}, programHeaderSize=${programHeaderSize.toWHex()}, " +
                "programHeaderCount=${programHeaderCount.toWHex()}, sectionHeaderOffset=${sectionHeaderOffset.toWHex()}, sectionHeaderSize=${sectionHeaderSize.toWHex()}, " +
                "sectionHeaderCount=${sectionHeaderCount.toWHex()})"
    }
}

class ElfProgramHeader(val offset: Int, val vAddr: Int, val memSize: Int) {
    override fun toString(): String {
        return "ElfProgramHeader(offset=${offset.toWHex()}, vAddr=${vAddr.toWHex()}, memSize=${memSize.toWHex()})"
    }
}

class ElfSectionHeader(val type: Int, val vAddr: Int, val offset: Int, val size: Int) {
    override fun toString(): String {
        return "ElfSectionHeader(type=${type.toWHex()}, vAddr=${vAddr.toWHex()}, offset=${offset.toWHex()}, size=${size.toWHex()})"
    }
}
