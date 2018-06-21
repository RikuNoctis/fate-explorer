package com.kotcrab.fate.patcher

import com.kotcrab.fate.file.PakFileEntry
import com.kotcrab.fate.io.FateOutputStream
import java.io.File

/**
 * Writes new .PAK file from specified entries. Only mode 0x8000 is supported (file paths included in output .pak).
 */
class PakWriter(val entries: List<PakFileEntry>, val outFile: File) {
    init {
        with(FateOutputStream(outFile)) {
            writeShort(entries.size)
            writeShort(0x8000) //paths included

            entries.forEach {
                writeInt(it.bytes.size)
            }

            align(16)

            entries.forEach { entry ->
                writeString(entry.path, 64)
                writeBytes(entry.bytes)
                align(16)
            }
            close()
        }
    }
}
