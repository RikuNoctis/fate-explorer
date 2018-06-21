package com.kotcrab.fate.file.extra

import com.kotcrab.fate.io.FateInputStream
import java.io.File

/** @author Kotcrab */
class ExtraInfoMatrixBinFile(dFile: File, tFile: File) {
    val entries = mutableListOf<ExtraInfoMatrixEntry>()

    init {
        val titles = mutableListOf<String>()
        val texts = mutableListOf<String>()
        val shortcuts = mutableListOf<String>()

        with(FateInputStream(dFile)) {
            val id = readInt()
            skip(0x50)
            repeat(3) {
                val text = readDatString(maintainStreamPos = true)
                titles.add(text)
                skip(0x20)
            }
            repeat(6) {
                val text = readDatString(maintainStreamPos = true)
                titles.add(text)
                skip(0x80)
            }
            close()
        }
        with(FateInputStream(tFile)) {
            val id = readInt()
            repeat(9) { _ ->
                val text = readDatString(maintainStreamPos = true, fixUpNewLine = true)
                texts.add(text)
                skip(0x1400)
            }
            repeat(9) { _ ->
                val text = readDatString(maintainStreamPos = true)
                shortcuts.add(text)
                skip(0x44)
            }
            close()
        }

        if (titles.size != texts.size || texts.size != shortcuts.size) error("infomatrix content count mismatch")
        repeat(titles.count()) { idx ->
            entries.add(ExtraInfoMatrixEntry(titles[idx], texts[idx], shortcuts[idx]))
        }
    }
}

class ExtraInfoMatrixEntry(val title: String, val text: String, val shortcut: String)
