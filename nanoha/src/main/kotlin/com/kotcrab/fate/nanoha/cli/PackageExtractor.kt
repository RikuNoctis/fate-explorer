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

package com.kotcrab.fate.nanoha.cli

import com.kotcrab.fate.nanoha.acesISOUnpack
import com.kotcrab.fate.nanoha.acesOutput
import com.kotcrab.fate.nanoha.file.LzsFile
import com.kotcrab.fate.nanoha.file.PacFile
import com.kotcrab.fate.nanoha.gearsISOUnpack
import com.kotcrab.fate.nanoha.gearsOutput
import kio.KioInputStream
import kio.util.child
import kio.util.walkDir
import kio.util.writeJson
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
            with(KioInputStream(entry.bytes)) {
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
