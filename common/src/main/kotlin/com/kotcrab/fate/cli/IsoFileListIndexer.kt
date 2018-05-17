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
