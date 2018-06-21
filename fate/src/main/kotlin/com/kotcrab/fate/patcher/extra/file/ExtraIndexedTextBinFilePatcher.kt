package com.kotcrab.fate.patcher.extra.file

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.io.FateOutputStream
import com.kotcrab.fate.patcher.extra.ExtraTranslation
import com.kotcrab.fate.util.WINDOWS_932
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraIndexedTextBinFilePatcher(origBytes: ByteArray, outFile: File, translation: ExtraTranslation, translationOffset: Int,
                                     charset: Charset = Charsets.WINDOWS_932) {
    init {
        val out = FateOutputStream(outFile)
        with(FateInputStream(origBytes)) {
            var entryCount = 0
            while (true) {
                val index = readInt()
                if ((index == 0 && entryCount > 0) || eof()) break
                val len = readInt()
                val text = readDatString(fixUpNewLine = true, charset = charset)
                val newText = translation.getTranslation(entryCount, translationOffset)
                entryCount++
                if (newText.isBlank()) {
                    out.writeInt(index)
                    out.writeInt(len)
                    out.writeDatString(text, charset = charset)
                } else {
                    out.writeInt(index)
                    val bytes = newText.toByteArray(charset)
                    out.writeInt(bytes.size + (4 - (bytes.size % 4)))
                    out.writeDatString(newText, charset = charset)
                }
                if (eof()) break
            }
            close()
        }
        out.align(16)
        out.close()
    }
}
