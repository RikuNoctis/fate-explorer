package com.kotcrab.fate.nanoha.editor.core

import com.kotcrab.fate.util.walkDir
import java.io.File
import java.util.*

/** @author Kotcrab */
class FtxtLoader {
    val entries = TreeMap<File, Array<IntArray>>()

    fun load(asmDir: File) {
        walkDir(asmDir, processFile = { asmFile ->
            val ftxt = FtxtDecoder(asmFile)
            entries[asmFile] = ftxt.texts
        })
    }

    fun getCount(): Int {
        return entries.values.sumBy { it.size }
    }

    fun getEntry(file: File, ftxtIndex: Int): IntArray? {
        val texts = entries[file] ?: return null
        return texts[ftxtIndex]
    }
}
