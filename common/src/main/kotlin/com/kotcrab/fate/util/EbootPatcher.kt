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

import com.kotcrab.fate.file.ElfFile
import kio.KioInputStream
import kio.util.arrayCopy
import kio.util.toWHex
import kmips.Assembler
import kmips.Endianness
import kmips.Reg
import kmips.Reg.ra
import kmips.Reg.sp
import kmips.assembleAsHexString
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.xml.bind.DatatypeConverter

@Suppress("UNUSED_VARIABLE")
/** @author Kotcrab */
class EbootPatcher(
    inFile: File,
    outFile: File,
    patchList: List<EbootPatch>,
    baseProgramHeaderIdx: Int,
    relocationSectionHeaderIdx: Int = -1
) {
    init {
        val elf = ElfFile(inFile)
        val baseProgramHeader = elf.programHeaders[baseProgramHeaderIdx]
        val nextProgramHeader = elf.programHeaders.getOrNull(baseProgramHeaderIdx + 1)

        val outBytes = inFile.readBytes()
        patchList.forEach { patch ->
            if (patch.active == false) return@forEach
            patch.changes.forEach { change ->
                if (nextProgramHeader != null && change.startAddr > nextProgramHeader.offset) {
                    error("Trying to patch unsafe address: ${change.startAddr.toWHex()}")
                }
                val patchBytes = DatatypeConverter.parseHexBinary(change.hexString)
                arrayCopy(src = patchBytes, dest = outBytes, destPos = change.startAddr + baseProgramHeader.offset)
            }
            if (relocationSectionHeaderIdx == -1 && patch.relocationsToRemove.isNotEmpty()) {
                error("To remove relocations you must specify index of relocation section header")
            }
            if (relocationSectionHeaderIdx != -1) {
                val relocationSectionHeader = elf.sectionHeaders[relocationSectionHeaderIdx]
                patch.relocationsToRemove.forEach relRemoveLoop@{ addrToRemove ->
                    with(KioInputStream(outBytes)) {
                        setPos(relocationSectionHeader.offset)
                        while (pos() < relocationSectionHeader.offset + relocationSectionHeader.size) {
                            val addr = readInt()
                            val typePos = pos()
                            val type = readInt()
                            if (addr == addrToRemove) {
                                arrayCopy(src = ByteArray(4), dest = outBytes, destPos = typePos)
                                close()
                                return@relRemoveLoop
                            }
                        }
                    }
                    error("Can't remove relocation: ${addrToRemove.toWHex()}, entry not found.")
                }
            }
        }
        outFile.writeBytes(outBytes)
    }
}

fun MutableCollection<EbootPatch>.patch(name: String, active: Boolean = true, init: EbootPatch.() -> Unit) {
    val patch = EbootPatch(name, active)
    patch.init()
    add(patch)
}

class EbootPatch(val name: String, var active: Boolean) {
    val changes: ArrayList<EbootChange> = ArrayList()
    val relocationsToRemove: ArrayList<Int> = ArrayList()

    fun change(startAddr: Int, init: Assembler.() -> Unit) {
        changes.add(EbootChange(startAddr, assembleAsHexString(startAddr + 0x8804000, Endianness.Little, init)))
    }

    fun removeRelocation(addr: Int) {
        relocationsToRemove.add(addr)
    }
}

class EbootChange(val startAddr: Int, val hexString: String)

fun Assembler.zeroTerminatedString(string: String): Int {
    val addr = virtualPc
    var zeroWritten = false
    for (i in 0..string.length step 4) {
        val defaultChar: (Int) -> Char = {
            zeroWritten = true
            0.toChar()
        }

        val char0 = string.getOrElse(i + 3, defaultChar).toInt() shl 24
        val char1 = string.getOrElse(i + 2, defaultChar).toInt() shl 16
        val char2 = string.getOrElse(i + 1, defaultChar).toInt() shl 8
        val char3 = string.getOrElse(i, defaultChar).toInt()
        data(char0 or char1 or char2 or char3)
    }
    if (zeroWritten == false) {
        data(0)
    }
    return addr
}

fun Assembler.float(value: Float) {
    val buf = ByteBuffer.allocate(4).putFloat(value)
    buf.rewind()
    data(buf.order(ByteOrder.BIG_ENDIAN).int)
}

fun Assembler.writeBytes(bytes: ByteArray) {
    if (bytes.size % 4 != 0) error("Buffer size is not aligned to word size")
    with(KioInputStream(bytes)) {
        while (!eof()) {
            data(readInt())
        }
    }
}

fun Assembler.word(intValue: Long): Int {
    val addr = virtualPc
    data(intValue.toInt())
    return addr
}

fun Assembler.word(value: Int): Int {
    val addr = virtualPc
    data(value)
    return addr
}

fun Assembler.preserve(regs: Array<Reg>): FunctionContext {
    val ctx = FunctionContext(this, regs)
    ctx.preserve()
    return ctx
}

class FunctionContext(private val assembler: Assembler, private val regs: Array<Reg>) {
    fun byteSize() = regs.size * 4

    fun preserve() = with(assembler) {
        addi(sp, sp, -regs.size * 4)
        regs.forEachIndexed { idx, reg ->
            sw(reg, idx * 4, sp)
        }
    }

    fun restore() = with(assembler) {
        restoreRegs()
        addi(sp, sp, regs.size * 4)
    }

    fun exit() = with(assembler) {
        jr(ra)
        nop()
    }

    fun restoreAndExit() = with(assembler) {
        restoreRegs()
        jr(ra)
        addi(sp, sp, regs.size * 4)
        nop()
    }

    private fun restoreRegs() = with(assembler) {
        regs.forEachIndexed { idx, reg ->
            lw(reg, idx * 4, sp)
        }
    }
}

