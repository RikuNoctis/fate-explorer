package com.kotcrab.fate.nanoha.cli

import com.kotcrab.fate.nanoha.file.FntFile
import com.kotcrab.fate.nanoha.gearsOutput
import com.kotcrab.fate.nanoha.gearsPACUnpack
import com.kotcrab.fate.util.child

/** @author Kotcrab */
fun main(args: Array<String>) {
    arrayOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12").forEach {
        println("Processing $it...")
        val font = gearsPACUnpack.child("story$it.fnt")
        FntFile(font).writeToPng(gearsOutput.child("story$it.png"))
    }
}
