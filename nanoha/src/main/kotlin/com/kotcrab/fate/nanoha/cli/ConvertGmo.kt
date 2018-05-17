package com.kotcrab.fate.nanoha.cli

import com.kotcrab.fate.nanoha.*
import com.kotcrab.fate.tex.GmoTextureConverter
import com.kotcrab.fate.util.child

/** @author Kotcrab */
fun main(args: Array<String>) {
    val convertAces = false

    if (convertAces) {
        val gameTools = acesTools
        val gimConv = gameTools.child("""GimConv\GimConv.exe""")
        GmoTextureConverter(gimConv, acesPACUnpack, { it.extension == "gmo" })
                .convertTo(acesOutput.child("texgmo"))
    } else {
        val gameTools = gearsTools
        val gimConv = gameTools.child("""GimConv\GimConv.exe""")
        GmoTextureConverter(gimConv, gearsPACUnpack, { it.extension == "gmo" })
                .convertTo(gearsOutput.child("texgmo"))
    }
}
