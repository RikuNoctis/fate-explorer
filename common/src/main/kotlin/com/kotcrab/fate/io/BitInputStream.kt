package com.kotcrab.fate.io

import com.kotcrab.fate.util.isBitSet
import com.kotcrab.fate.util.toInt

/** @author Kotcrab */
class BitInputStream(private val bytes: ByteArray, private val msbOrder: Boolean = true) {
    private var currentByte = bytes[0]
    var posInCurrentByte = 0
        private set
    var pos = 0
        private set

    fun readBit(): Boolean {
        val value = currentByte.isBitSet(if (msbOrder) 7 - posInCurrentByte else posInCurrentByte)
        posInCurrentByte++
        if (posInCurrentByte == 8) {
            pos++
            posInCurrentByte = 0
            currentByte = bytes[pos]
        }
        return value
    }

    fun readByte(): Byte {
        var value = 0
        repeat(8) { it ->
            value = value or (readBit().toInt() shl (7 - it))
        }
        return value.toByte()
    }

    fun readInt(bits: Int = 32): Int {
        if (bits > 32) error("bits must be <=32")
        var value = 0
        repeat(bits) { it ->
            value = value or (readBit().toInt() shl (bits - 1 - it))
        }
        return value
    }
}
