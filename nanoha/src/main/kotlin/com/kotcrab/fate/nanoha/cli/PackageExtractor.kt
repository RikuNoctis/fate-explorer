package com.kotcrab.fate.nanoha.cli

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.nanoha.acesISOUnpack
import com.kotcrab.fate.nanoha.acesOutput
import com.kotcrab.fate.nanoha.file.LzsFile
import com.kotcrab.fate.nanoha.file.PacFile
import com.kotcrab.fate.nanoha.gearsISOUnpack
import com.kotcrab.fate.nanoha.gearsOutput
import com.kotcrab.fate.util.child
import com.kotcrab.fate.util.walkDir
import com.kotcrab.fate.util.writeJson
import java.io.File

/** @author Kotcrab */
fun main(args: Array<String>) {
    val unpackAces = false

    if (unpackAces) {
        extractGamePackage(acesISOUnpack, acesOutput, acesOutput.child("pacunpack"))
    } else {
        extractGamePackage(gearsISOUnpack, gearsOutput, gearsOutput.child("pacunpack"))
    }
}

private fun extractGamePackage(isoSrc: File, jsonOutDir: File, filesOutDir: File) {
    jsonOutDir.mkdirs()
    filesOutDir.mkdirs()
    val files = mutableMapOf<String, MutableList<String>>()
    val lzsMap = mutableMapOf<String, String>()

    walkDir(isoSrc, { file ->
        if (file.extension != "pac") return@walkDir
        PacFile(file).entries.forEach { entry ->
            files.getOrPut(entry.fileName, { mutableListOf() }).add(file.name)
            with(FateInputStream(entry.bytes)) {
                val outFile = filesOutDir.child(entry.fileName)
                val bytesToWrite = if (readStringAndTrim(4) == "LZS") {
                    val lzs = LzsFile(entry.bytes)
                    lzsMap[entry.fileName] = lzs.fileName
                    lzs.decompressedBytes
                } else {
                    entry.bytes
                }
                if (outFile.exists() && outFile.readBytes().contentEquals(bytesToWrite) == false) {
                    error("Duplicate file with different content ${entry.fileName}")
                }
                println("Writing ${entry.fileName}...")
                outFile.writeBytes(bytesToWrite)
                close()
            }
        }
    })
    jsonOutDir.child("pac_files.json").writeJson(files)
    jsonOutDir.child("lzs_map.json").writeJson(lzsMap)
}
