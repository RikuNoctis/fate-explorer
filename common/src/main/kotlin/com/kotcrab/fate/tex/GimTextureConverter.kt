package com.kotcrab.fate.tex

import com.kotcrab.fate.util.child
import com.kotcrab.fate.util.execute
import com.kotcrab.fate.util.relativizePath
import com.kotcrab.fate.util.walkDir
import java.io.File

/** @author Kotcrab */
class GimTextureConverter(private val gimConvExe: File,
                          private val srcDir: File,
                          private val fileFilter: (File) -> Boolean) {
    fun convertTo(outDir: File) {
        outDir.mkdirs()
        var convertedCount = 0
        walkDir(srcDir, {
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
