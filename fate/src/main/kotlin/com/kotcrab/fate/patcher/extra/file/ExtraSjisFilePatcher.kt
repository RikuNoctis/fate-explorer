package com.kotcrab.fate.patcher.extra.file

import com.kotcrab.fate.file.SjisEntry
import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.io.FateOutputStream
import com.kotcrab.fate.patcher.extra.ExtraTranslation
import com.kotcrab.fate.util.WINDOWS_932
import java.io.EOFException
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraSjisFilePatcher(origBytes: ByteArray, origJpBytes: ByteArray,
                           outFile: File, translation: ExtraTranslation, translationOffset: Int,
                           charset: Charset = Charsets.WINDOWS_932) {
    init {
        val out = FateOutputStream(outFile)
        var entryCount = 0
        val jpIn = FateInputStream(origJpBytes)
        with(FateInputStream(origBytes)) {
            while (!eof()) {
                try {
                    val l1 = readNullTerminatedString(charset)
                    val l2 = readNullTerminatedString(charset)
                    val l3 = readNullTerminatedString(charset)
                    val l4 = readNullTerminatedString(charset)
                    val l5 = readNullTerminatedString(charset)
                    val jpl1 = jpIn.readNullTerminatedString(charset)
                    val jpl2 = jpIn.readNullTerminatedString(charset)
                    val jpl3 = jpIn.readNullTerminatedString(charset)
                    val jpl4 = jpIn.readNullTerminatedString(charset)
                    val jpl5 = jpIn.readNullTerminatedString(charset)
                    val texts = arrayOf(l1, l2, l3, l4, l5)
                    if (texts.all { it.isNotBlank() }) {
                        if (SjisEntry(l1, l2, l3, l4, l5).isUnused() &&
                                SjisEntry(jpl1, jpl2, jpl3, jpl4, jpl5).isUnused()) {
                            out.writeNullTerminatedString(l1, charset = charset)
                            out.writeNullTerminatedString(l2, charset = charset)
                            out.writeNullTerminatedString(l3, charset = charset)
                            out.writeNullTerminatedString(l4, charset = charset)
                            out.writeNullTerminatedString(l5, charset = charset)
                        } else {
                            texts.forEach { text ->
                                val newText = translation.getTranslation(entryCount, translationOffset)
                                if (newText.isBlank()) {
                                    out.writeNullTerminatedString(text, charset)
                                } else {
                                    out.writeNullTerminatedString(newText, charset)
                                }
                                entryCount++
                            }
                        }
                    } else {
                        out.writeNullTerminatedString(l1, charset = charset)
                        out.writeNullTerminatedString(l2, charset = charset)
                        out.writeNullTerminatedString(l3, charset = charset)
                        out.writeNullTerminatedString(l4, charset = charset)
                        out.writeNullTerminatedString(l5, charset = charset)
                    }
                } catch (ignored: EOFException) {
                }
            }
            close()
        }
        out.align(16)
        out.close()
    }
}
