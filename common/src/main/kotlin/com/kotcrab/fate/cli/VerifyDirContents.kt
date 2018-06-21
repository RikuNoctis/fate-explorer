package com.kotcrab.fate.cli

import com.google.common.io.Files
import com.kotcrab.fate.util.relativizePath
import java.io.File
import java.util.*
import kotlin.system.exitProcess

/** @author Kotcrab */
fun main(args: Array<String>) {
    if (args.size != 2) {
        println("usage: [dir1] [dir2]")
        return
    }
    val in1 = File(args[0])
    val in2 = File(args[1])
    VerifyDirContents(in1, in2)
}

class VerifyDirContents(in1: File, in2: File) {
    init {
        if (!in1.exists()) error("dir1 does not exist")
        if (!in2.exists()) error("dir2 does not exist")

        println("Checking files, this may take a while...")
        for (sourceFile in Files.fileTreeTraverser().preOrderTraversal(in1)) {
            if (sourceFile.isDirectory) continue

            val relativePath = sourceFile.relativizePath(in1)
            val compareFile = File(in2, relativePath)

            print("Check $relativePath... ")
            if (!Arrays.equals(sourceFile.readBytes(), compareFile.readBytes())) {
                println("content mismatch!")
                exitProcess(1)
            }
            println("ok")

            sourceFile.readBytes()
        }

        println("Done, all files matches.")
    }
}
