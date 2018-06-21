package com.kotcrab.fate.file.extra

import com.kotcrab.fate.io.FateInputStream
import java.io.File

/**
 * @author Kotcrab
 */
class ExtraItemParam04BinFile(bytes: ByteArray, jpSize: Boolean) {
    constructor(file: File, jpSize: Boolean) : this(file.readBytes(), jpSize)

    val entries: Array<ExtraItemParam04Entry>

    init {
        val itemEntries = mutableListOf<ExtraItemParam04Entry>()

        with(FateInputStream(bytes)) {
            val entries = readInt()
            readInt()
            readInt()
            readInt()
            repeat(entries) { _ ->
                val entryBytes = readBytes(if (jpSize) 0xB8 else 0xC0)
                with(FateInputStream(entryBytes)) itemParse@{
                    val name = readDatString(maintainStreamPos = true)
                    skip(0x48)
                    val buyValue = readInt()
                    val sellValue = readInt()
                    val desc = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x30 else 0x34)
                    val trivia = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x28 else 0x34)
                    itemEntries.add(ExtraItemParam04Entry(entryBytes, name, buyValue, sellValue, desc, trivia))
                }
            }
            close()
        }

        entries = itemEntries.toTypedArray()
    }
}

class ExtraItemParam04Entry(val bytes: ByteArray, val name: String, val buyValue: Int, val sellValue: Int,
                            val description: String, val trivia: String) {
    fun isUnused(): Boolean = name in listOf("（予備）", "-")
}
