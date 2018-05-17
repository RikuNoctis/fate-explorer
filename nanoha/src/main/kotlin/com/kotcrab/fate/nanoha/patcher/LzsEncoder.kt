package com.kotcrab.fate.nanoha.patcher

import com.kotcrab.fate.io.FateOutputStream
import com.kotcrab.fate.util.padArray
import java.io.ByteArrayOutputStream
import java.io.File

/** @author Kotcrab */
class LzsEncoder(inFile: File, lzsName: String) {
    var compressedBytes: ByteArray

    init {
        val lzsNameLen = with(FateOutputStream(ByteArrayOutputStream())) {
            writeDatString(lzsName)
            getAsByteArrayOutputStream().toByteArray().size
        }

        val bs = ByteArrayOutputStream()
        with(FateOutputStream(bs)) {
            val comprOffset = 0x20 + lzsNameLen
            val inBytes = padArray(inFile.readBytes())
            val compr = ByteArray(inBytes.size / 8, { 0xFF.toByte() })
            val dictOffset = comprOffset + compr.size
            val dict = inBytes

            writeString("LZS", 4)
            writeInt(0x80505) //magic
            writeInt(comprOffset) //comr offset
            writeInt(dictOffset) //dict offset

            writeInt(inBytes.size) //decompressed size
            writeInt(dictOffset + dict.size)//lzs file size
            writeInt(0x0200) //magic
            writeInt(0) //dummy

            writeDatString(lzsName)
            writeBytes(compr) //compr
            writeBytes(dict) //dict
        }
        compressedBytes = bs.toByteArray()
    }
}
