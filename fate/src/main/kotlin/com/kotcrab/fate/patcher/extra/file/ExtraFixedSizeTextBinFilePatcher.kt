package com.kotcrab.fate.patcher.extra.file

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.io.FateOutputStream
import com.kotcrab.fate.patcher.extra.ExtraTranslation
import com.kotcrab.fate.util.WINDOWS_932
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraFixedSizeTextBinFilePatcher(origBytes: ByteArray, outFile: File, translation: ExtraTranslation,
                                       translationOffset: Int, charset: Charset = Charsets.WINDOWS_932) {
    init {
        val out = FateOutputStream(outFile)
        with(FateInputStream(origBytes)) {
            var entryCount = 0

            val fileEntryCount = readInt()
            val unkInt = readInt()
            out.writeInt(fileEntryCount)
            out.writeInt(unkInt)

            repeat(fileEntryCount) {
                val index = readInt()
                val text = readDatString(maintainStreamPos = true, charset = charset)
                readBytes(0x40)

                val newText = translation.getTranslation(entryCount, translationOffset)
                entryCount++

                if (newText.isBlank()) {
                    out.writeInt(index)
                    val len = out.writeDatString(text, charset = charset)
                    out.writeBytes(0x40 - len)
                } else {
                    out.writeInt(index)
                    val len = out.writeDatString(newText, charset = charset)
                    if (len > 0x40) error("too long text: '$newText' for FixedSizeTextBinFile, max length 0x40 bytes")
                    out.writeBytes(0x40 - len)
                }
            }
            close()
        }
        out.align(16)
        out.close()
    }
}
