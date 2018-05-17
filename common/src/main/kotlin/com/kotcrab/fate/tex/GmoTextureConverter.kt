package com.kotcrab.fate.tex

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.util.*
import java.io.File

/** @author Kotcrab */
class GmoTextureConverter(private val gimConvExe: File,
                          private val srcDir: File,
                          private val fileFilter: (File) -> Boolean) {
    fun convertTo(outDir: File) {
        outDir.mkdirs()
        val tmpGim = outDir.child("tmp.gim")
        var convertedGmoCount = 0
        var convertedGimCount = 0
        val gimMagic = "MIG.00.1PSP".toByteArray()

        walkDir(srcDir, {
            if (fileFilter(it)) {
                println("Processing ${it.relativizePath(srcDir)}")
                val gmoBytes = it.readBytes()
                val gmoInput = FateInputStream(gmoBytes)
                val textures = mutableListOf<ByteArray>()
                var startFrom = 0
                while (true) {
                    val texPos = getSubArrayPos(gmoBytes, gimMagic, startFrom)
                    if (texPos == -1) break
                    gmoInput.setPos(texPos - 0x4)
                    val texSize = gmoInput.readInt()
                    textures.add(gmoInput.readBytes(texSize))
                    startFrom = gmoInput.count()
                }
                println("Found ${textures.size} textures")
                textures.forEachIndexed { index, bytes ->
                    tmpGim.writeBytes(bytes)
                    execute(gimConvExe, arrayOf(tmpGim, "-o",
                            outDir.child("${it.relativizePath(srcDir).replace("/", "$")}!tex$index.png")))
                    convertedGimCount++
                }
                convertedGmoCount++
                println()
            }
        })

        tmpGim.delete()
        println("Processed $convertedGmoCount files")
        println("Should have created $convertedGimCount files")
    }
}
