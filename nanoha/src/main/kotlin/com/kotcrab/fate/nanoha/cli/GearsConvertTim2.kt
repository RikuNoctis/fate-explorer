package com.kotcrab.fate.nanoha.cli

import com.kotcrab.fate.nanoha.file.Tim2File
import com.kotcrab.fate.nanoha.gearsOutput
import com.kotcrab.fate.nanoha.gearsPACUnpack
import com.kotcrab.fate.util.child

fun main(args: Array<String>) {
    val out = gearsOutput.child("textm2")
    out.mkdirs()
    gearsPACUnpack.listFiles().forEach {
        if (it.extension != "tm2") return@forEach
        println("Processing ${it.name}...")
        Tim2File(it).writeToPng(out.child("${it.nameWithoutExtension}.tm2.png"))
    }
}
