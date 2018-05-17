package com.kotcrab.fate.io

class SequentialArrayReader(public val bytes: ByteArray) {
    public var pos = 0
        private set
    val size = bytes.size

    fun read(): Byte {
        val byte = bytes[pos]
        pos++
        return byte
    }

    fun read(n: Int): ByteArray {
        val outBytes = ByteArray(n)
        repeat(n) {
            outBytes[it] = read()
        }
        return outBytes
    }
}

class SequentialArrayWriter(public val bytes: ByteArray) {
    public var pos = 0
        private set
    val size = bytes.size

    fun write(byte: Byte) {
        bytes[pos] = byte
        pos++
    }

    fun write(inBytes: ByteArray) {
        inBytes.forEach { write(it) }
    }

    operator fun get(idx: Int): Byte {
        return bytes[idx]
    }
}
