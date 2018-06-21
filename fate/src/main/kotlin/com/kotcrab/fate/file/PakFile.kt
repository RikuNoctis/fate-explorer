package com.kotcrab.fate.file

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.util.padArray
import java.io.File
import java.nio.file.Files

/**
 * .PAK file parser
 * @author Kotcrab
 */
class PakFile(bytes: ByteArray) {
    constructor(file: File) : this(file.readBytes())

    var headerSize: Int = 0
        private set
    val entries: Array<PakFileEntry>

    init {
        val fileEntries = mutableListOf<PakFileEntry>()

        with(FateInputStream(bytes)) {
            val filesCount = readShort().toInt()
            val pathIncluded = readShort() == 0x8000.toShort()

            val fileSizes = mutableListOf<Int>()
            repeat(filesCount) {
                fileSizes.add(readInt())
            }

            align(16)
            headerSize = count()

            repeat(filesCount) { index ->
                val path = if (pathIncluded) readString(64).replace("\u0000", "") else ""
                fileEntries.add(PakFileEntry(index, path, readBytes(fileSizes[index])))
            }

            close()
        }

        entries = fileEntries.toTypedArray()
    }

    fun getEntry(path: String): PakFileEntry {
        return entries.getEntry(path)
    }
}

fun Array<PakFileEntry>.sizeInGame(): Int = this.map { it.bytes.size + 0x40 }.sum()

fun Array<PakFileEntry>.getLocationInGame(baseAddress: Int, path: String): Int {
    var address = baseAddress
    forEach { entry ->
        if (entry.path == path) return address
        address += entry.bytes.size + 0x40
    }
    error("No such entry: $path")
}

fun Array<PakFileEntry>.getEntry(path: String): PakFileEntry {
    forEachIndexed { idx, entry ->
        if (entry.path == path) {
            return this[idx]
        }
    }
    error("No such entry: $path")
}

fun Array<PakFileEntry>.getEntryBytes(path: String): ByteArray = getEntry(path).bytes

fun Array<PakFileEntry>.replaceEntry(path: String, newBytes: ByteArray) {
    forEachIndexed { idx, entry ->
        if (entry.path == path) {
            this[idx] = PakFileEntry(this[idx].index, path, padArray(newBytes))
            return
        }
    }
    error("No such entry: $path")
}

fun Array<PakFileEntry>.replaceEntry(id: Int, newPath: String, newBytes: ByteArray) {
    forEachIndexed { idx, _ ->
        if (idx == id) {
            this[idx] = PakFileEntry(this[idx].index, newPath, padArray(newBytes))
            return
        }
    }
    error("No such entry with id: $id")
}

class PakFileEntry(val index: Int, val path: String, val bytes: ByteArray) {
    fun writeToFile(outFile: File) {
        outFile.parentFile.mkdirs()
        outFile.writeBytes(bytes)
    }

    fun writeToTmpFile(): File {
        val ext: String
        val extPos = path.lastIndexOf('.')
        ext = if (extPos != -1) {
            path.substring(extPos)
        } else {
            ".tmp"
        }
        val file = Files.createTempFile("FateExplorer", ext).toFile()
        file.writeBytes(bytes)
        file.deleteOnExit()
        return file
    }
}
