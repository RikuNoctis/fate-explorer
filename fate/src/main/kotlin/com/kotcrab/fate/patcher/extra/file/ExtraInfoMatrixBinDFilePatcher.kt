package com.kotcrab.fate.patcher.extra.file

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.io.FateOutputStream
import com.kotcrab.fate.patcher.extra.ExtraTranslation
import com.kotcrab.fate.util.WINDOWS_932
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraInfoMatrixBinDFilePatcher(origBytes: ByteArray, outFile: File, translation: ExtraTranslation, translationOffset: Int,
                                     charset: Charset = Charsets.WINDOWS_932) {
    init {
        val out = FateOutputStream(outFile)
        var entryCount = 0
        with(FateInputStream(origBytes)) {
            out.writeBytes(readBytes(0x54))
            repeat(3) {
                val len = out.writeDatString(translation.getTranslation(entryCount, translationOffset),
                        charset = charset)
                if (len > 0x20) error("Too long translation text")
                out.writeBytes(0x20 - len)
                entryCount++
                entryCount++
                entryCount++
                skip(0x20)
            }
            repeat(6) {
                val len = out.writeDatString(translation.getTranslation(entryCount, translationOffset),
                        charset = charset)
                if (len > 0x80) error("Too long translation text")
                out.writeBytes(0x80 - len)
                entryCount++
                entryCount++
                entryCount++
                skip(0x80)
            }
            out.writeBytes(readBytes((size - longCount()).toInt()))
            close()
        }
        out.align(16)
        out.close()
    }
}
