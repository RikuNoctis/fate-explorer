package com.kotcrab.fate.file.extra

import com.kotcrab.fate.io.FateInputStream
import java.io.File

/**
 * @author Kotcrab
 */
class ExtraItemParam01BinFile(bytes: ByteArray, jpSize: Boolean) {
    constructor(file: File, jpSize: Boolean) : this(file.readBytes(), jpSize)

    val entries: Array<ExtraItemParam01Entry>

    init {
        val itemEntries = mutableListOf<ExtraItemParam01Entry>()

        with(FateInputStream(bytes)) {
            val entries = readInt()
            readInt()
            readInt()
            readInt()
            repeat(entries) {
                val entryBytes = readBytes(if (jpSize) 0xE4 else 0x100)
                with(FateInputStream(entryBytes)) itemParse@{
                    val name = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x54 else 0x54)
                    val buyValue = readInt()
                    val sellValue = readInt()
                    val desc = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x30 else 0x34)
                    val trivia1 = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x30 else 0x34)
                    val trivia2 = readDatString(maintainStreamPos = true)
                    skip(if (jpSize) 0x28 else 0x34)
                    itemEntries.add(ExtraItemParam01Entry(entryBytes, name, buyValue, sellValue, desc, trivia1, trivia2))
                }
            }
            close()
        }

        entries = itemEntries.toTypedArray()
    }
}

class ExtraItemParam01Entry(val bytes: ByteArray, val name: String, val buyValue: Int, val sellValue: Int,
                            val description: String, val trivia1: String, val trivia2: String) {
    fun isUnused(): Boolean = name == "（予備）"
}
