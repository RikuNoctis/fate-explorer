package com.kotcrab.fate.io

import com.google.common.io.CountingOutputStream
import com.google.common.io.LittleEndianDataOutputStream
import com.kotcrab.fate.WINDOWS_932
import java.io.*
import java.nio.charset.Charset

/** @author Kotcrab */
class FateOutputStream(private val outputStream: OutputStream, littleEndian: Boolean = true) {
    constructor(file: File, littleEndian: Boolean = true) : this(FileOutputStream(file), littleEndian)

    private val counter = CountingOutputStream(outputStream)
    private val stream: FilterOutputStream = if (littleEndian) LittleEndianDataOutputStream(counter) else DataOutputStream(counter)
    private val output: DataOutput = stream as DataOutput

    fun writeShort(value: Int) {
        output.writeShort(value)
    }

    fun writeInt(value: Int) {
        output.writeInt(value)
    }

    fun writeLong(value: Long) {
        output.writeLong(value)
    }

    fun writeFloat(value: Float) {
        output.writeFloat(value)
    }

    fun writeZeroTerminatedString(string: String, charset: Charset = Charsets.WINDOWS_932) {
        output.write(string.toByteArray(charset))
        output.writeByte(0)
    }

    fun writeString(string: String, length: Int = string.length) {
        output.writeBytes(string)
        writeBytes(length - string.length)
    }

    fun writeDatString(string: String, charset: Charset = Charsets.WINDOWS_932): Int {
        val bytes = string.toByteArray(charset)
        output.write(bytes)
        val padding = 4 - (bytes.size % 4)
        writeBytes(ByteArray(padding))
        return bytes.size + padding
    }

    fun writeBytes(count: Int) {
        output.write(ByteArray(count))
    }

    fun writeBytes(bytes: ByteArray) {
        output.write(bytes)
    }

    fun align(pad: Long) {
        if (longCount() % pad == 0L) return
        val targetCount = (longCount() / pad + 1) * pad
        writeBytes((targetCount - longCount()).toInt())
    }


    fun longCount(): Long {
        return counter.count
    }

    fun count(): Int {
        return longCount().toInt()
    }

    fun close() {
        stream.close()
    }

    fun getAsByteArrayOutputStream(): ByteArrayOutputStream {
        return outputStream as ByteArrayOutputStream
    }
}
