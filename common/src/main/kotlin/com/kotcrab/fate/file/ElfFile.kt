package com.kotcrab.fate.file

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.util.toHex
import com.kotcrab.fate.util.toUnsignedInt
import java.io.File

class ElfFile(val bytes: ByteArray) {
    constructor(file: File) : this(file.readBytes())

    lateinit var header: ElfHeader
        private set
    val programHeaders: Array<ElfProgramHeader>
    val sectionHeaders: Array<ElfSectionHeader>

    init {
        val programHeaders = mutableListOf<ElfProgramHeader>()
        val sectionHeaders = mutableListOf<ElfSectionHeader>()

        with(FateInputStream(bytes)) {

            if (readByte().toUnsignedInt() != 0x7F || readString(3) != "ELF") error("Not an ELF file")
            header = ElfHeader(
                    entryPoint = readInt(at = 0x18),
                    programHeaderOffset = readInt(at = 0x1C),
                    programHeaderSize = readShort(at = 0x2A).toInt(),
                    programHeaderCount = readShort(at = 0x2C).toInt(),
                    sectionHeaderOffset = readInt(at = 0x20),
                    sectionHeaderSize = readShort(at = 0x2E).toInt(),
                    sectionHeaderCount = readShort(at = 0x30).toInt())

            setPos(header.programHeaderOffset)
            repeat(header.programHeaderCount) {
                val progBytes = readBytes(header.programHeaderSize)
                with(FateInputStream(progBytes)) {
                    programHeaders.add(ElfProgramHeader(
                            offset = readInt(at = 0x04),
                            vAddr = readInt(at = 0x08),
                            memSize = readInt(at = 0x14)))
                }
            }

            setPos(header.sectionHeaderOffset)
            repeat(header.sectionHeaderCount) {
                val sectBytes = readBytes(header.sectionHeaderSize)
                with(FateInputStream(sectBytes)) {
                    sectionHeaders.add(ElfSectionHeader(
                            type = readInt(at = 0x04),
                            vAddr = readInt(at = 0x0C),
                            offset = readInt(at = 0x10),
                            size = readInt(at = 0x14)))
                }
            }
        }

        this.programHeaders = programHeaders.toTypedArray()
        this.sectionHeaders = sectionHeaders.toTypedArray()
    }


}

class ElfHeader(val entryPoint: Int,
                val programHeaderOffset: Int, val programHeaderSize: Int, val programHeaderCount: Int,
                val sectionHeaderOffset: Int, val sectionHeaderSize: Int, val sectionHeaderCount: Int) {
    override fun toString(): String {
        return "ElfHeader(entryPoint=${entryPoint.toHex()}, programHeaderOffset=${programHeaderOffset.toHex()}, programHeaderSize=${programHeaderSize.toHex()}, " +
                "programHeaderCount=${programHeaderCount.toHex()}, sectionHeaderOffset=${sectionHeaderOffset.toHex()}, sectionHeaderSize=${sectionHeaderSize.toHex()}, " +
                "sectionHeaderCount=${sectionHeaderCount.toHex()})"
    }
}

class ElfProgramHeader(val offset: Int, val vAddr: Int, val memSize: Int) {
    override fun toString(): String {
        return "ElfProgramHeader(offset=${offset.toHex()}, vAddr=${vAddr.toHex()}, memSize=${memSize.toHex()})"
    }
}

class ElfSectionHeader(val type: Int, val vAddr: Int, val offset: Int, val size: Int) {
    override fun toString(): String {
        return "ElfSectionHeader(type=${type.toHex()}, vAddr=${vAddr.toHex()}, offset=${offset.toHex()}, size=${size.toHex()})"
    }
}
