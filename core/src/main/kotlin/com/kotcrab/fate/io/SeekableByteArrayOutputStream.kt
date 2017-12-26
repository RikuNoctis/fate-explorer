package com.kotcrab.fate.io

import com.kotcrab.fate.toUnsignedInt
import java.io.ByteArrayOutputStream

/** @author Kotcrab */
class SeekableByteArrayOutputStream : ByteArrayOutputStream {
    constructor() : super()
    constructor(size: Int) : super(size)

    fun seek(pos: Int) {
        count = pos
    }

    fun write(byte: Byte) {
        super.write(byte.toUnsignedInt())
    }

    fun at(pos: Int): Byte {
        return buf[pos]
    }

    fun count(): Int {
        return super.count
    }

    @Synchronized override fun toByteArray(): ByteArray {
        return if (count == buf.size) buf else super.toByteArray()
    }

    fun getBuf(): ByteArray {
        return buf
    }
}
