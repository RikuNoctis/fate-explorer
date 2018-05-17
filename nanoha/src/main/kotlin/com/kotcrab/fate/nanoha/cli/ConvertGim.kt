package com.kotcrab.fate.nanoha.cli

import com.kotcrab.fate.nanoha.*
import com.kotcrab.fate.tex.GimTextureConverter
import com.kotcrab.fate.util.child

/** @author Kotcrab */
fun main(args: Array<String>) {
    val convertAces = false

    if (convertAces) {
        val gameTools = acesTools
        val gimConv = gameTools.child("""GimConv\GimConv.exe""")
        GimTextureConverter(gimConv, acesPACUnpack, { it.extension == "gim" })
                .convertTo(acesOutput.child("texgim"))
    } else {
        val gameTools = gearsTools
        val gimConv = gameTools.child("""GimConv\GimConv.exe""")
        println("Converting GIM files")
        GimTextureConverter(gimConv, gearsPACUnpack, { it.extension == "gim" })
                .convertTo(gearsOutput.child("texgim"))
        println("Converting TGA files")
        GimTextureConverter(gimConv, gearsPACUnpack, { it.extension == "tga" })
                .convertTo(gearsOutput.child("textga"))
    }
}
