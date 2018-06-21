package com.kotcrab.fate

import com.kotcrab.fate.file.CmpFile
import com.kotcrab.fate.file.PakFile
import com.kotcrab.fate.util.Log
import com.kotcrab.fate.util.readableFileSize
import java.io.File
import java.util.*

/**
 * Batch file extractor for Extra and CCC .pak/.cmp files. Use this after extracting main CPK.
 * @author Kotcrab
 */
class PackageExtractor(srcDir: File, outDir: File, log: Log = Log()) {
    init {
        var pakFiles = 0
        var pakFileEntries = 0
        var duplicates = 0
        var duplicatesSize = 0L
        val duplicatesWithContentCollision = mutableListOf<String>()

        val files = srcDir.walk().toList().filter { it.extension == "pak" || it.extension == "cmp" }
        files.forEachIndexed { idx, file ->
            val data = when (file.extension) {
                "cmp" -> CmpFile(file).getData()
                "pak" -> file.readBytes()
                else -> {
                    return@forEachIndexed
                }
            }
            log.info("${idx + 1}/${files.size} Process ${file.name}")
            val pak = PakFile(data)
            pakFiles++
            pak.entries.forEachIndexed { pakIdx, entry ->
                var outName = entry.path
                if (entry.path == "") {
                    log.warn("Anonymous PAK file entry in PAK ${file.name}, ID $pakIdx")
                    outName = "__anonymous from ${file.name} file id $pakIdx"
                }
                var outFile = File(outDir, outName)
                if (outFile.isDirectory) {
                    log.warn("PAK entry points to existing directory")
                    outName = "__directory from ${file.name} file id $pakIdx"
                    outFile = File(outDir, outName)
                }
                if (outFile.exists()) {
                    val outFileData = outFile.readBytes()
                    if (Arrays.equals(entry.bytes, outFileData)) {
                        log.warn("Duplicate PAK file entry $outName in PAK ${file.name}")
                        duplicates++
                        duplicatesSize += entry.bytes.size
                    } else {
                        duplicatesWithContentCollision.add(outName)
                        log.warn("Duplicate PAK file entry $outName with collision name in PAK ${file.name}")
                        for (i in 2..Integer.MAX_VALUE) {
                            outFile = File(outFile.parentFile.path, "${outFile.nameWithoutExtension} ($i).${outFile.extension}")
                            if (outFile.exists() == false) break
                        }
                    }
                } else {
                    pakFileEntries++
                }
                entry.writeToFile(outFile)
            }
        }

        log.info("Processed files: $pakFiles")
        log.info("Unpacked files: $pakFileEntries")
        log.info("Duplicate files: $duplicates")
        log.info("Duplicate files size: ${readableFileSize(duplicatesSize)}")
        log.info("Duplicate files with content collision: ${duplicatesWithContentCollision.size}")
        if (duplicatesWithContentCollision.size != 0) {
            log.info("Duplicate files with content collision list:")
            log.info(duplicatesWithContentCollision.joinToString(separator = "\n"))
        }
        log.info("Done")
    }
}

