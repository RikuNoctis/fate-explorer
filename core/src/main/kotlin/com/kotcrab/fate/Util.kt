package com.kotcrab.fate

import com.google.common.io.ByteStreams
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.PumpStreamHandler
import java.io.File
import java.io.Reader
import java.nio.charset.Charset

/** @author Kotcrab */

fun Byte.toUnsignedInt() = (this.toInt() and 0xFF)

fun Short.toUnsignedInt() = (this.toInt() and 0xFFFF)

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

fun File.child(name: String): File {
    return File(this, name)
}

val stdGson = createStdGson()

fun createStdGson(prettyPrint: Boolean = true): Gson {
    return if (prettyPrint) {
        GsonBuilder().setPrettyPrinting().create()
    } else {
        GsonBuilder().create()
    }
}

inline fun <reified T> Gson.fromJson(reader: Reader) = this.fromJson<T>(reader, object : TypeToken<T>() {}.type)

inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

fun File.writeJson(src: Any) {
    bufferedWriter().use { stdGson.toJson(src, it) }
}

fun File.writeJson(gson: Gson = stdGson, src: Any) {
    bufferedWriter().use { gson.toJson(src, it) }
}

inline fun <reified T> File.readJson(gson: Gson = stdGson): T {
    return bufferedReader().use { gson.fromJson(it) }
}

fun execute(executable: File, args: Array<Any>, workingDirectory: File? = null, exitValue: Int = 0,
            streamHandler: PumpStreamHandler? = null) {
    val cmdLine = CommandLine(executable.absolutePath)
    args.forEachIndexed { index, _ ->
        cmdLine.addArgument("\${arg$index}")
    }
    val map = mutableMapOf<String, Any>()
    args.forEachIndexed { index, arg ->
        map.put("arg$index", arg)
    }
    cmdLine.substitutionMap = map
    val executor = DefaultExecutor()
    if (workingDirectory != null) executor.workingDirectory = workingDirectory
    if (streamHandler != null) executor.streamHandler = streamHandler
    executor.setExitValue(exitValue)
    executor.execute(cmdLine)
}

fun nullStreamHandler() = PumpStreamHandler(ByteStreams.nullOutputStream(), ByteStreams.nullOutputStream())
