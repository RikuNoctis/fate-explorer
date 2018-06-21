package com.kotcrab.fate.file

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.util.WINDOWS_932
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class IndexedTextBinFile(file: File, charset: Charset = Charsets.WINDOWS_932) {
    val entries = mutableListOf<Pair<Int, String>>()

    init {
        with(FateInputStream(file)) {
            var lastIndex = 0
            while (true) {
                val index = readInt()
                if (index < lastIndex) break
                val length = readInt()
                if (length == 0) break
                val text = readDatString(fixUpNewLine = true, charset = charset)
                entries.add(index to text)
                lastIndex++
                if (eof()) break
            }
            close()
        }
    }
}

