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

package com.kotcrab.fate.cli

import java.io.File

/** @author Kotcrab */
fun main(args: Array<String>) {
    if (args.size != 2) {
        println("usage: [inputFile] [outputFile]")
        return
    }
    val input = File(args[0])
    val output = File(args[1])
    IsoFileListIndexer(input, output)
}

class IsoFileListIndexer(inputFile: File, outputFile: File) {
    init {
        if (!inputFile.exists()) error("input file does not exist")
        val sb = StringBuilder()
        val lines = inputFile.readLines()
        lines.forEachIndexed { idx, file ->
            sb.append("$file ${lines.size - idx}\r\n")
        }
        outputFile.writeText(sb.toString())
    }
}
