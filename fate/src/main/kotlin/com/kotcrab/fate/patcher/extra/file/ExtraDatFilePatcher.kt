package com.kotcrab.fate.patcher.extra.file

import com.kotcrab.fate.io.FateOutputStream
import com.kotcrab.fate.io.LERandomAccessFile
import com.kotcrab.fate.io.align
import com.kotcrab.fate.io.writeDatString
import com.kotcrab.fate.patcher.extra.ExtraTranslation
import java.io.ByteArrayOutputStream
import java.io.File

/** @author Kotcrab */
class ExtraDatFilePatcher(origBytes: ByteArray, outFile: File, translation: ExtraTranslation, origTranslation: ExtraTranslation,
                          entries: List<CombinedDatEntry>) {
    init {
        outFile.writeBytes(origBytes)
        val raf = LERandomAccessFile(outFile, "rw")

        var modified = false
        val attachedTextMap = mutableMapOf<String, Long>()
        entries.forEach {
            var tnIndex = origTranslation.enTexts.indexOf(it.enEntry.text)
            if (tnIndex == -1) {
                println("WARN: DAT Missing ${it.enEntry.text}")
                return@forEach
            }
            if (tnIndex == 1763) {
                if (it.jpEntry.text.startsWith("#C120200255ありす#CDEFは#C255120120アリス#CDEF。")) {
                    tnIndex = 1764
                }
            }
            val en = translation.getTranslation(tnIndex)
            if (en != it.enEntry.text) {
                val enByteLen = FateOutputStream(ByteArrayOutputStream()).writeDatString(en)
                val origByteLen = FateOutputStream(ByteArrayOutputStream()).writeDatString(it.enEntry.text)
                if (enByteLen <= origByteLen) {
                    raf.seek(it.enEntry.textLoc.toLong())
                    raf.writeDatString(en)
                    modified = true
                } else {
                    var attachedPointer = attachedTextMap[en]
                    if (attachedPointer == null) {
                        attachedPointer = raf.length()
                        raf.seek(raf.length())
                        raf.writeDatString(en)
                        attachedTextMap[en] = attachedPointer
                    }
                    raf.seek(it.enEntry.pointerLoc.toLong())
                    raf.writeInt(attachedPointer.toInt())
                    modified = true
                }
            }
        }
        raf.seek(raf.length())
        raf.align(16)
        raf.close()
        if (!modified) {
            outFile.delete()
        }
    }
}

class CombinedDatEntry(val relPath: String, val enEntry: DatEntry, val jpEntry: DatEntry)

class DatEntry(val text: String, val textLoc: Int, val pointerLoc: Int)
