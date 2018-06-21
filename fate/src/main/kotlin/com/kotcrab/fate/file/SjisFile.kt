package com.kotcrab.fate.file

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.util.WINDOWS_932
import java.io.EOFException
import java.io.File

/** @author Kotcrab */
class SjisFile(bytes: ByteArray) {
    constructor(file: File) : this(file.readBytes())

    val entries = mutableListOf<SjisEntry>()

    init {
        with(FateInputStream(bytes)) {
            while (!eof()) {
                try {
                    val l1 = readNullTerminatedString(Charsets.WINDOWS_932)
                    val l2 = readNullTerminatedString(Charsets.WINDOWS_932)
                    val l3 = readNullTerminatedString(Charsets.WINDOWS_932)
                    val l4 = readNullTerminatedString(Charsets.WINDOWS_932)
                    val l5 = readNullTerminatedString(Charsets.WINDOWS_932)
                    if (arrayOf(l1, l2, l3, l4, l5).all { it.isNotBlank() }) {
                        entries.add(SjisEntry(l1, l2, l3, l4, l5))
                    }
                } catch (ignored: EOFException) {
                    // assuming file can have some padding
                }
            }
            close()
        }
    }
}

class SjisEntry(val l1: String, val l2: String, val l3: String, val l4: String, val l5: String) {
    fun isUnused(): Boolean = l1 in listOf("（予備）", "0") && l2 in listOf("（予備）", "0") && l3 == "0" && l4 == "0" && l5 == "0"
}
