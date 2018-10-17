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

package com.kotcrab.fate.file.extella

import com.google.common.collect.Ordering
import com.kotcrab.fate.util.Log
import kio.KioInputStream
import kio.util.child
import java.io.File
import java.util.zip.Inflater

/** @author Kotcrab */
class PkFile(pkFile: File, outDir: File, val log: Log = Log()) {
    init {
        if (outDir.exists() && outDir.list().isNotEmpty()) log.fatal("outDir is not empty")
        outDir.mkdirs()

        val pfsFile = pkFile.resolveSibling("${pkFile.nameWithoutExtension}.pfs")
        val pkhFile = pkFile.resolveSibling("${pkFile.nameWithoutExtension}.pkh")

        if (!pfsFile.exists()) log.fatal("PFS file does not exist")
        if (!pkhFile.exists()) log.fatal("PKH file does not exist")

        val pfsDirData = mutableListOf<PfsDirEntry>()
        val pfsOffsetData = mutableListOf<PfsOffsetEntry>()
        val pfsNameData = mutableListOf<PfsNameEntry>()

        var dirCount: Int = -1
        var pfsFileCount: Int = -1

        log.info("Reading file dictionary...")
        with(KioInputStream(pfsFile, littleEndian = false)) {
            readInt()
            readInt()
            dirCount = readInt()
            pfsFileCount = readInt()
            println("$dirCount directories, $pfsFileCount files")

            repeat(dirCount) {
                pfsDirData.add(PfsDirEntry(readInt(), readInt(), readInt(), readInt(), readInt(), readInt()))
            }
            repeat(dirCount + pfsFileCount) {
                pfsOffsetData.add(PfsOffsetEntry(readInt()))
            }
            repeat(dirCount + pfsFileCount) {
                pfsNameData.add(PfsNameEntry(readNullTerminatedString(Charsets.US_ASCII)))
            }
            close()
        }

        log.info("Reading archive file map...")
        val pkhData = mutableListOf<PkhEntry>()
        with(KioInputStream(pkhFile, littleEndian = false)) {
            val pkhFileCount = readInt()
            val orderCheck = Ordering.from<PkhEntry> { o1, o2 ->
                Integer.compareUnsigned(o1.lowercasePathHash, o2.lowercasePathHash)
            }

            repeat(pkhFileCount) {
                pkhData.add(PkhEntry(readInt(), readIntUnsigned(), readInt(), readInt()))
            }
            if (orderCheck.isOrdered(pkhData)) {
                log.info("Detected Fate/Extella format")
            } else {
                setPos(0x4)
                pkhData.clear()
                repeat(pkhFileCount) {
                    val pathHash = readInt()
                    readInt() // always 0 for Link
                    readInt() // always 0 for Link
                    val offset = readIntUnsigned()
                    val decompressedSize = readInt()
                    val compSize = readInt()
                    pkhData.add(PkhEntry(pathHash, offset, decompressedSize, compSize))
                }
                if (orderCheck.isOrdered(pkhData)) {
                    log.info("Detected Fate/Extella Link format")
                } else {
                    log.fatal("Can't auto detect .pkh format")
                }
            }
            close()
        }

        log.info("Creating directory structure...")
        val extractedDirMap = mutableMapOf<Int, File>()
        pfsDirData.forEachIndexed { index, entry ->
            if (entry.parentDirId != -1) {
                val parentDir = extractedDirMap[entry.parentDirId] ?: log.fatal("Inconsistent directory structure")
                val dir = parentDir.child(pfsNameData[index].name)
                dir.mkdir()
                extractedDirMap.put(entry.dirId, dir)
            } else {
                extractedDirMap.put(entry.dirId, outDir)
            }
        }

        log.info("Extracting files...")
        pkhData.sortBy { it.pkOffset }
        val pkArchive = KioInputStream(pkFile, littleEndian = false)
        var extractedFileCount = 0
        pfsDirData.forEach { entry ->
            val dir = extractedDirMap[entry.dirId]!!

            repeat(entry.childFileCount) { fileIndex ->
                val fileName = pfsNameData[dirCount + entry.startChildFile + fileIndex].name
                val outFile = dir.child(fileName)
                val relPath = outFile.relativeTo(outDir)
                val progress = "${extractedFileCount + 1}/$pfsFileCount"

                val pathHash = crc32(relPath.toString().replace("\\", "/").toLowerCase().toByteArray())
                val fileDescriptor = pkhData.firstOrNull { it.lowercasePathHash == pathHash }
                        ?: log.fatal("Can't find file descriptor")
                pkArchive.setPos(fileDescriptor.pkOffset)
                if (fileDescriptor.compressedSize == 0) {
                    println("$progress Extract $relPath...")
                    val fileBytes = pkArchive.readBytes(fileDescriptor.decompressedSize)
                    outFile.writeBytes(fileBytes)
                } else {
                    println("$progress Extract and decompress $relPath...")
                    val compressedBytes = pkArchive.readBytes(fileDescriptor.compressedSize)
                    outFile.writeBytes(compressedBytes)

                    val decompressor = Inflater()
                    decompressor.setInput(compressedBytes, 0, compressedBytes.size)
                    val result = ByteArray(fileDescriptor.decompressedSize)
                    decompressor.inflate(result)
                    decompressor.end()
                    outFile.writeBytes(result)
                }
                extractedFileCount++
            }
        }

        log.info("Archive extracted")
    }

    private fun crc32(bytes: ByteArray): Int {
        return PkCrc32.get(bytes, bytes.size)
    }

    private data class PfsDirEntry(val dirId: Int, val parentDirId: Int, val startChildDir: Int, val childDirCount: Int,
                                   val startChildFile: Int, val childFileCount: Int)

    private data class PfsOffsetEntry(val offset: Int)

    private data class PfsNameEntry(val name: String)

    private data class PkhEntry(val lowercasePathHash: Int, val pkOffset: Long, val decompressedSize: Int, val compressedSize: Int)

    private fun KioInputStream.readIntUnsigned(): Long {
        return readInt().toLong() and 0xffffffffL
    }
}
