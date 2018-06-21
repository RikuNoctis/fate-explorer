package com.kotcrab.fate.file

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.util.WINDOWS_932
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class FixedSizeTextBinFile(file: File, charset: Charset = Charsets.WINDOWS_932) {
    val entries = mutableListOf<Pair<Int, String>>()

    init {
        with(FateInputStream(file)) {
            val entryCount = readInt()
            readInt()
            repeat(entryCount) {
                val index = readInt()
                val text = readDatString(maintainStreamPos = true, charset = charset)
                skip(0x40)
                entries.add(index to text)
            }
            close()
        }
    }
}

