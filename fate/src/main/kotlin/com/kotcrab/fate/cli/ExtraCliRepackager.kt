package com.kotcrab.fate.cli

import com.kotcrab.fate.fateBase
import com.kotcrab.fate.patcher.extra.ExtraRepackager
import com.kotcrab.fate.util.child
import java.io.File

fun main(args: Array<String>) {
    val toolkit = if(args.isEmpty()) fateBase.child("Extra Toolkit") else File(args.first())
    ExtraRepackager(
            toolkit,
            skipTranslationInsertion = false,
            skipCpkCreation = false
    ).buildAll()
}
