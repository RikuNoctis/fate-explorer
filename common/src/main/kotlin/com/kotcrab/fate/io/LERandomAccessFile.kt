package com.kotcrab.fate.io

import com.kotcrab.fate.util.WINDOWS_932
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

class LERandomAccessFile(file: File, mode: String) : DataInput, DataOutput {
    constructor(path: String, mode: String) : this(File(path), mode)

    private var raf = RandomAccessFile(file, mode)

    val fd
        get() = raf.fd

    val filePointer
        get() = raf.filePointer

    fun length(): Long {
        return raf.length()
    }

    fun close() {
        raf.close()
    }

    fun seek(pos: Long) {
        raf.seek(pos)
    }

    override fun skipBytes(n: Int): Int {
        return raf.skipBytes(n)
    }

    fun read(): Int {
        return raf.read()
    }

    fun read(b: ByteArray): Int {
        return raf.read(b)
    }

    fun read(b: ByteArray, off: Int, len: Int): Int {
        return raf.read(b, off, len)
    }

    override fun readFully(b: ByteArray) {
        raf.readFully(b, 0, b.size)
    }

    override fun readFully(b: ByteArray, off: Int, len: Int) {
        raf.readFully(b, off, len)
    }

    override fun readByte(): Byte {
        return raf.readByte()
    }

    override fun readBoolean(): Boolean {
        return raf.readBoolean()
    }

    override fun readUnsignedByte(): Int {
        return raf.readUnsignedByte()
    }

    override fun readShort(): Short {
        val b0 = read()
        val b1 = read()
        return ((b1 shl 8) or b0).toShort()
    }

    override fun readUnsignedShort(): Int {
        val b0 = read()
        val b1 = read()
        return (b1 shl 8) or b0
    }

    override fun readChar(): Char {
        val b0 = read()
        val b1 = read()
        return ((b1 shl 8) or b0).toChar()
    }

    override fun readInt(): Int {
        val b0 = read()
        val b1 = read()
        val b2 = read()
        val b3 = read()
        return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    }

    override fun readLong(): Long {
        val b0 = read()
        val b1 = read()
        val b2 = read()
        val b3 = read()
        val b4 = read()
        val b5 = read()
        val b6 = read()
        val b7 = read()
        return (b7.toLong() shl 56) or (b6.toLong() shl 48) or (b5.toLong() shl 40) or (b4.toLong() shl 32) or
                (b3.toLong() shl 24) or (b2.toLong() shl 16) or (b1.toLong() shl 8) or b0.toLong()
    }

    override fun readFloat(): Float {
        return Float.fromBits(readInt())
    }

    override fun readDouble(): Double {
        return Double.fromBits(readLong())
    }

    override fun readLine(): String {
        return raf.readLine()
    }

    @Deprecated("Use some other method for reading strings", level = DeprecationLevel.ERROR)
    override fun readUTF(): String {
        return raf.readUTF()
    }

    override fun write(b: Int) {
        raf.write(b)
    }

    override fun write(b: ByteArray) {
        raf.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        raf.write(b, off, len)
    }

    override fun writeBoolean(v: Boolean) {
        raf.writeBoolean(v)
    }

    override fun writeByte(v: Int) {
        raf.writeByte(v)
    }

    override fun writeShort(v: Int) {
        raf.writeByte(v)
        raf.writeByte(v shr 8)
    }

    override fun writeChar(v: Int) {
        raf.writeByte(v)
        raf.writeByte(v shr 8)
    }

    override fun writeInt(v: Int) {
        raf.writeByte(v)
        raf.writeByte(v shr 8)
        raf.writeByte(v shr 16)
        raf.writeByte(v shr 24)
    }

    override fun writeLong(v: Long) {
        raf.writeByte((v and 0xFF).toInt())
        raf.writeByte((v shr 8 and 0xFF).toInt())
        raf.writeByte((v shr 16 and 0xFF).toInt())
        raf.writeByte((v shr 24 and 0xFF).toInt())
        raf.writeByte((v shr 32 and 0xFF).toInt())
        raf.writeByte((v shr 40 and 0xFF).toInt())
        raf.writeByte((v shr 48 and 0xFF).toInt())
        raf.writeByte((v shr 56 and 0xFF).toInt())
    }

    override fun writeFloat(v: Float) {
        writeInt(v.toBits())
    }

    override fun writeDouble(v: Double) {
        writeLong(v.toBits())
    }

    override fun writeBytes(s: String) {
        raf.writeBytes(s)
    }

    override fun writeChars(s: String) {
        s.toCharArray().forEach { writeChar(it.toInt()) }
    }

    @Deprecated("Use some other method for writing strings", level = DeprecationLevel.ERROR)
    override fun writeUTF(s: String) {
        raf.writeUTF(s)
    }
}

fun LERandomAccessFile.writeDatString(string: String, charset: Charset = Charsets.WINDOWS_932): Int {
    val bytes = string.toByteArray(charset)
    write(bytes)
    val padding = 4 - (bytes.size % 4)
    write(ByteArray(padding))
    return bytes.size + padding
}

fun LERandomAccessFile.align(pad: Long) {
    if (length() % pad == 0L) return
    val targetCount = (length() / pad + 1) * pad
    write(ByteArray((targetCount - length()).toInt()))
}
