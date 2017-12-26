package com.kotcrab.fate.file

import com.kotcrab.fate.Log
import com.kotcrab.fate.child
import com.kotcrab.fate.io.BitInputStream
import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.io.SeekableByteArrayOutputStream
import com.kotcrab.fate.toUnsignedInt
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.experimental.buildSequence

/**
 * .CPK unpacker and CRILAYLA decompressor.
 * Unpacking ported from QuickBMS's CPK.BMS v22 (newer were crashing QuickBMS)
 * Decompressor ported from arc_unpacker
 * @author Kotcrab
 * */
class CpkFile(file: File, outDir: File, private val log: Log = Log(), ignoreNotEmptyOut: Boolean = false) {
    private companion object {
        const val COLUMN_STORAGE_MASK = 0xf0
        const val COLUMN_TYPE_MASK = 0x0f

        const val COLUMN_STORAGE_CONSTANT = 0x30
        const val COLUMN_STORAGE_ZERO = 0x10

        const val COLUMN_TYPE_DATA = 0x0b
        const val COLUMN_TYPE_STRING = 0x0a
        const val COLUMN_TYPE_FLOAT = 0x08
        const val COLUMN_TYPE_8BYTE2 = 0x07
        const val COLUMN_TYPE_8BYTE = 0x06
        const val COLUMN_TYPE_4BYTE2 = 0x05
        const val COLUMN_TYPE_4BYTE = 0x04
        const val COLUMN_TYPE_2BYTE2 = 0x03
        const val COLUMN_TYPE_2BYTE = 0x02
        const val COLUMN_TYPE_1BYTE2 = 0x01
        const val COLUMN_TYPE_1BYTE = 0x00
    }

    init {
        if (ignoreNotEmptyOut == false && outDir.exists() && outDir.list().isNotEmpty()) log.fatal("outDir is not empty")
        outDir.mkdirs()

        val input = FateInputStream(file, littleEndian = false)
        with(input) {
            if (readString(4) != "CPK ") log.fatal("No CPK magic string inside input file")
            val tocOffset = queryUtf(input, 0, 0, "TocOffset") as Long
            val contentOffset = queryUtf(input, 0, 0, "ContentOffset") as Long
            val files = queryUtf(input, 0, 0, "Files") as Int
            log.info("TOC at $tocOffset")
            log.info("Content at $contentOffset")
            log.info("Files: $files")

            setPos(tocOffset)
            if (readString(4) != "TOC ") log.fatal("No TOC signature at offset $tocOffset")

            log.startProgress()
            repeat(files) { index ->
                val dirName = queryUtf(input, tocOffset, index, "DirName") as String
                val fileName = queryUtf(input, tocOffset, index, "FileName") as String
                val fileSize = queryUtf(input, tocOffset, index, "FileSize") as Int
                val extractSize = queryUtf(input, tocOffset, index, "ExtractSize") as Int
                val fileOffset = queryUtf(input, tocOffset, index, "FileOffset") as Long
                val path = "$dirName/$fileName"
                val outFile = outDir.child(path)
                outFile.parentFile.mkdirs()
                outFile.createNewFile()

                val baseOffset = fileOffset + tocOffset

                setPos(baseOffset)
                if (extractSize > fileSize) {
                    log.progress(index, files, "Extract and decompress $path")
                    val compressedBytes = readBytes(fileSize)
                    val decompressedBytes = decompressLayla(compressedBytes, extractSize)
                    outFile.writeBytes(decompressedBytes)
                } else {
                    log.progress(index, files, "Extract $path")
                    outFile.writeBytes(readBytes(fileSize))
                }
            }
            log.endProgress()

            close()
        }

        log.info("Done")
    }

    private fun decompressLayla(fileBytes: ByteArray, extractSize: Int): ByteArray {
        val input = FateInputStream(fileBytes)
        if (input.readString(8) != "CRILAYLA") log.fatal("Not compressed using CRILAYLA")
        val sizeOrig = input.readInt()
        val sizeComp = input.readInt()
        val dataComp = input.readBytes(sizeComp)
        val prefix = input.readBytes(0x100)
        input.close()

        dataComp.reverse()

        val out = SeekableByteArrayOutputStream(sizeOrig)

        with(BitInputStream(dataComp)) {
            while (out.size() < sizeOrig) {
                val sizes = buildSequence {
                    yield(2)
                    yield(3)
                    yield(5)
                    while (true) yield(8)
                }

                if (readBit()) {
                    var repetitions = 3
                    val lookBehind = readInt(13) + 3

                    for (size in sizes) {
                        val marker = readInt(size)
                        repetitions += marker
                        if (marker != (1 shl size) - 1) {
                            break
                        }
                    }

                    repeat(repetitions) {
                        val byte = out.at(out.size() - lookBehind)
                        out.write(byte)
                    }
                } else {
                    val byte = readByte()
                    out.write(byte)
                }

                if (sizeComp - 1 == pos && posInCurrentByte == 7) {
                    break
                }
            }
        }

        val combined = ByteArrayOutputStream(extractSize)
        combined.write(prefix)
        combined.write(out.toByteArray().reversedArray())
        return combined.toByteArray()
    }

    private fun queryUtf(input: FateInputStream, initialOffset: Long, index: Int, name: String): Any = with(input) {
        val offset = initialOffset + 0x10
        setPos(offset)
        if (readString(4) != "@UTF") log.fatal("No UTF table at $offset")
        val table = UtfTable()
        table.tableOffset = offset
        table.tableSize = readInt()
        table.schemaOffset = 0x20
        table.rowsOffset = readInt()
        table.stringTableOffset = readInt()
        table.dataOffset = readInt()
        table.nameString = readInt()
        table.columns = readShort()
        table.rowWidth = readShort()
        table.rows = readInt()

        val schemas = arrayOfNulls<UtfColumnSchema>(table.columns.toInt())

        repeat(table.columns.toInt()) { index ->
            val schema = UtfColumnSchema()
            schemas[index] = schema
            schema.type = readByte().toUnsignedInt()
            schema.columnName = readInt()

            if (schema.type and COLUMN_STORAGE_MASK == COLUMN_STORAGE_CONSTANT) {
                schema.constantOffset = longCount()
                val typeFlag = schema.type and COLUMN_TYPE_MASK
                when (typeFlag) {
                    COLUMN_TYPE_STRING -> readString(4)
                    COLUMN_TYPE_DATA -> readString(8)
                    COLUMN_TYPE_FLOAT -> readString(4)
                    COLUMN_TYPE_8BYTE2 -> readString(8)
                    COLUMN_TYPE_8BYTE -> readString(8)
                    COLUMN_TYPE_4BYTE2 -> readString(4)
                    COLUMN_TYPE_4BYTE -> readString(4)
                    COLUMN_TYPE_2BYTE2 -> readString(2)
                    COLUMN_TYPE_2BYTE -> readString(2)
                    COLUMN_TYPE_1BYTE2 -> readString(1)
                    COLUMN_TYPE_1BYTE -> readString(1)
                    else -> error("Unknown constant type: $typeFlag")
                }
            }
        }

        val baseOffset = table.stringTableOffset + 0x8 + offset

        for (i in index until table.rows) {
            val widthOffset = i * table.rowWidth
            var rowOffset = table.tableOffset + 8 + table.rowsOffset + widthOffset

            repeat(table.columns.toInt()) { index ->
                val schema = schemas[index]!!

                val dataOffset = if (schema.constantOffset >= 0) {
                    schema.constantOffset
                } else {
                    rowOffset
                }

                val oldReadAddr: Long
                val readBytes: Long

                val value: Any
                if (schema.type and COLUMN_STORAGE_MASK == COLUMN_STORAGE_ZERO) {
                    value = 0
                } else {
                    setPos(dataOffset)
                    val typeFlag = schema.type and COLUMN_TYPE_MASK
                    oldReadAddr = longCount()
                    when (typeFlag) {
                        COLUMN_TYPE_STRING -> {
                            val stringOffset = readInt()
                            setPos(baseOffset + stringOffset)
                            value = readNullTerminatedString()
                            readBytes = 4L
                        }
                        COLUMN_TYPE_DATA -> {
                            val varDataOffset = readInt()
                            val varDataSize = readInt()
                            setPos(baseOffset + varDataOffset)
                            value = readBytes(varDataSize)
                            readBytes = 8L
                        }
                        COLUMN_TYPE_FLOAT -> {
                            value = readFloat()
                            readBytes = 4L
                        }
                        COLUMN_TYPE_8BYTE, COLUMN_TYPE_8BYTE2 -> {
                            value = readLong()
                            readBytes = 8L
                        }
                        COLUMN_TYPE_4BYTE, COLUMN_TYPE_4BYTE2 -> {
                            value = readInt()
                            readBytes = 4L
                        }
                        COLUMN_TYPE_2BYTE, COLUMN_TYPE_2BYTE2 -> {
                            value = readShort()
                            readBytes = 2L
                        }
                        COLUMN_TYPE_1BYTE, COLUMN_TYPE_1BYTE2 -> {
                            value = readByte()
                            readBytes = 1L
                        }
                        else -> log.fatal("Unknown constant type: $typeFlag")
                    }

                    if (schema.constantOffset < 0) {
                        rowOffset = oldReadAddr + readBytes
                    }
                }

                setPos(baseOffset + schema.columnName)
                if (readNullTerminatedString() == name) {
                    return value
                }
            }
        }
        log.fatal("No UTF match")
    }

    private class UtfTable {
        var tableOffset = 0L
        var tableSize = 0
        var schemaOffset = 0
        var rowsOffset = 0
        var stringTableOffset = 0
        var dataOffset = 0
        var nameString = 0
        var columns: Short = 0
        var rowWidth: Short = 0
        var rows = 0
    }

    private class UtfColumnSchema {
        var type = 0
        var columnName = 0
        var constantOffset = -1L
    }
}
