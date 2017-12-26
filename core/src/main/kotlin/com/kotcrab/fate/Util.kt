package com.kotcrab.fate

import java.nio.charset.Charset

/** @author Kotcrab */

fun Byte.toUnsignedInt() = (this.toInt() and 0xFF)

fun Byte.getBits(): BooleanArray {
    val bits = BooleanArray(8)
    for (bit in 0..7) {
        bits[bit] = this.isBitSet(bit)
    }
    return bits
}

fun Byte.toHex() = String.format("%02X", this)
fun Short.toHex() = String.format("%03X", this)
fun Int.toHex() = String.format("%04X", this)
fun Long.toHex() = String.format("%08X", this)

fun Byte.isBitSet(bit: Int): Boolean {
    if (bit >= 8) throw IllegalArgumentException("Out of range, bit must be <8")
    return (this.toUnsignedInt()) and (1 shl bit) != 0
}

fun Int.isBitSet(bit: Int): Boolean {
    if (bit >= 32) throw IllegalArgumentException("Out of range, bit must be <32")
    return this and (1 shl bit) != 0
}

fun Byte.setBit(bit: Int): Byte {
    if (bit >= 8) throw IllegalArgumentException("Out of range, bit must be <8")
    return (this.toUnsignedInt() or (1 shl bit)).toByte()
}

fun Byte.resetBit(bit: Int): Byte {
    if (bit >= 8) throw IllegalArgumentException("Out of range, bit must be <8")
    return (this.toUnsignedInt() and (1 shl bit).inv()).toByte()
}

fun Byte.toggleBit(bit: Int): Byte {
    if (bit >= 8) throw IllegalArgumentException("Out of range, bit must be <8")
    return (this.toUnsignedInt() xor (1 shl bit)).toByte()
}

fun Boolean.toInt(): Int = if (this) 1 else 0

private val windows932Charset = Charset.forName("windows-932")
private val shiftJisCharset = Charset.forName("Shift_JIS")

val Charsets.WINDOWS_932: Charset
    get() = windows932Charset

val Charsets.SHIFT_JIS: Charset
    get() = shiftJisCharset
