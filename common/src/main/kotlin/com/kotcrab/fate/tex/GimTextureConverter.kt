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

package com.kotcrab.fate.tex

import kio.util.child
import kio.util.execute
import kio.util.relativizePath
import kio.util.walkDir
import java.io.File

/** @author Kotcrab */
class GimTextureConverter(private val gimConvExe: File,
                          private val srcDir: File,
                          private val fileFilter: (File) -> Boolean) {
    fun convertTo(outDir: File) {
        outDir.mkdirs()
        var convertedCount = 0
        walkDir(srcDir, processFile = {
            if (fileFilter(it)) {
                if (it.extension == "gim") {
                    execute(gimConvExe, arrayOf(it, "-o",
                            outDir.child("${it.relativizePath(srcDir).replace("/", "$")}.png")))
                    convertedCount++
                } else {
                    val gimFile = it.resolveSibling("${it.nameWithoutExtension}.gim")
                    it.renameTo(gimFile)
                    execute(gimConvExe, arrayOf(gimFile, "-o",
                            outDir.child("${it.relativizePath(srcDir).replace("/", "$")}.png")))
                    convertedCount++
                    gimFile.renameTo(it)
                }
            }
        })
        println("Should have created $convertedCount files")
    }
}
