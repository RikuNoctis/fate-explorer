package com.kotcrab.fate.io

import com.kotcrab.fate.util.toUnsignedInt
import java.io.ByteArrayOutputStream

/** @author Kotcrab */
class FastByteArrayOutputStream : ByteArrayOutputStream {
    constructor() : super()
    constructor(size: Int) : super(size)

    @Synchronized
    fun write(byte: Byte) {
        super.write(byte.toUnsignedInt())
    }

    @Synchronized
    fun at(pos: Int): Byte {
        return buf[pos]
    }

    @Synchronized
    fun count(): Int {
        return super.count
    }

    @Synchronized
    override fun toByteArray(): ByteArray {
        return if (count == buf.size) buf else super.toByteArray()
    }

    fun getInternalBuf(): ByteArray {
        return buf
    }
}
