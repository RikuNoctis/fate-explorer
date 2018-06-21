package com.kotcrab.fate.patcher.extra.file

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.io.FateOutputStream
import com.kotcrab.fate.patcher.extra.ExtraTranslation
import com.kotcrab.fate.util.WINDOWS_932
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraInfoMatrixBinTFilePatcher(origBytes: ByteArray, outFile: File, translation: ExtraTranslation, translationOffset: Int,
                                     charset: Charset = Charsets.WINDOWS_932) {
    init {
        val out = FateOutputStream(outFile)
        var entryCount = 0
        with(FateInputStream(origBytes)) {
            out.writeInt(readInt())
            repeat(9) {
                entryCount++
                entryCount++
                val len = out.writeDatString(translation.getTranslation(entryCount, translationOffset),
                        charset = charset)
                if (len > 0x1400) error("Too long translation text")
                out.writeBytes(0x1400 - len)
                entryCount++
            }
            entryCount = 0
            repeat(9) {
                entryCount++
                val len = out.writeDatString(translation.getTranslation(entryCount, translationOffset),
                        charset = charset)
                if (len > 0x44) error("Too long translation text")
                out.writeBytes(0x44 - len)
                entryCount++
                entryCount++
            }

            entryCount = 0

            close()
        }
        out.align(16)
        out.close()
    }
}
