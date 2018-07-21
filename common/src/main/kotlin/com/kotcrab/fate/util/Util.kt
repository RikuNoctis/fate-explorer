/*
 * Copyright 2017-2018 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kotcrab.fate.util

import com.google.common.io.BaseEncoding
import com.google.common.io.ByteStreams
import com.google.common.reflect.TypeToken
import com.google.gson.*
import com.kotcrab.fate.io.LERandomAccessFile
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.PumpStreamHandler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.io.Reader
import java.lang.reflect.Type
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.file.Paths
import java.text.DecimalFormat


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

fun Int.lowBits(): Int = this and 0xFFFF
fun Int.highBits(): Int = this ushr 16
fun Int.toLittleEndian(): Int {
    val buffer = ByteBuffer.allocate(4).putInt(this)
    buffer.rewind()
    return buffer.order(ByteOrder.LITTLE_ENDIAN).int
}

private val windows932Charset = Charset.forName("windows-932")
private val shiftJisCharset = Charset.forName("Shift_JIS")

val Charsets.WINDOWS_932: Charset
    get() = windows932Charset

@Deprecated(level = DeprecationLevel.WARNING, message = "Prefer using Charsets.WINDOWS_932 to properly support IBM code page 932",
        replaceWith = ReplaceWith("Charsets.WINDOWS_932", "com.kotcrab.fate.util.WINDOWS_932"))
val Charsets.SHIFT_JIS: Charset
    get() = shiftJisCharset

fun RandomAccessFile.seek(pos: Int) {
    seek(pos.toLong())
}

fun LERandomAccessFile.seek(pos: Int) {
    seek(pos.toLong())
}

fun RandomAccessFile.readBytes(n: Int): ByteArray {
    val bytes = ByteArray(n)
    read(bytes)
    return bytes
}

fun RandomAccessFile.skipNullBytes() {
    skipByte(0)
}

fun RandomAccessFile.skipByte(byteToSkip: Int) {
    while (true) {
        if (filePointer == length()) return
        val byte = readByte().toUnsignedInt()
        if (byte != byteToSkip) {
            seek(filePointer - 1L)
            return
        }
    }
}

fun RandomAccessFile.readNullTerminatedString(charset: Charset = Charsets.US_ASCII): String {
    val out = ByteArrayOutputStream()
    while (true) {
        val byte = readByte().toInt()
        if (byte == 0) break
        out.write(byte)
    }
    return String(out.toByteArray(), charset)
}

fun File.relativizePath(base: File): String {
    val pathAbsolute = Paths.get(this.absolutePath.toString())
    val pathBase = Paths.get(base.absolutePath.toString())
    val pathRelative = pathBase.relativize(pathAbsolute)
    var path = pathRelative.toString().replace("\\", "/")
    if (this.isDirectory) path += "/"
    return path
}

fun File.child(name: String): File {
    return File(this, name)
}

fun walkDir(dir: File, processFile: (File) -> Unit, errorHandler: (File, Exception) -> Unit = { _, e -> throw(e) }) {
    dir.listFiles().forEach {
        if (it.isFile) {
            try {
                processFile(it)
            } catch (e: Exception) {
                errorHandler(it, e)
            }
        } else {
            walkDir(it, processFile, errorHandler)
        }
    }
}

fun StringBuilder.appendWindowsLine(text: String = "") {
    appendLine(text, "\r\n")
}

fun StringBuilder.appendLine(text: String = "", newLine: String = "\n") {
    append(text)
    append(newLine)
}

val stdGson = createStdGson()

fun createStdGson(prettyPrint: Boolean = true, byteArrayAsBase64: Boolean = true): Gson {
    val builder = GsonBuilder()
    if (prettyPrint) {
        builder.setPrettyPrinting()
    }
    if (byteArrayAsBase64) {
        builder.registerTypeAdapter(ByteArray::class.java, ByteArrayAsBase64TypeAdapter())
    }
    return builder.create()
}

private class ByteArrayAsBase64TypeAdapter : JsonSerializer<ByteArray>, JsonDeserializer<ByteArray> {
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ByteArray {
        return BaseEncoding.base64().decode(json.asString)
    }

    override fun serialize(src: ByteArray, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(BaseEncoding.base64().encode(src))
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

fun execute(executable: File, args: Array<Any> = arrayOf(), workingDirectory: File? = null, exitValue: Int = 0,
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

fun getSubArrayPos(data: ByteArray, needle: ByteArray, startFrom: Int = 0): Int {
    outer@ for (i in startFrom..data.size - needle.size) {
        for (j in needle.indices) {
            if (data[i + j] != needle[j]) {
                continue@outer
            }
        }
        return i
    }
    return -1
}

fun RandomAccessFile.getSubArrayPos(needle: ByteArray, startFrom: Int = 0): Long {
    seek(0)
    val needleInts = needle.map { it.toUnsignedInt() }
    outer@ for (i in startFrom..length() - needle.size) {
        for (j in needle.indices) {
            seek(i + j)
            if (read() != needleInts[j]) {
                continue@outer
            }
        }
        return i
    }
    return -1
}

fun arrayCopy(src: ByteArray, srcPos: Int = 0, dest: ByteArray, destPos: Int = 0, length: Int = src.size) {
    System.arraycopy(src, srcPos, dest, destPos, length)
}

fun padArray(src: ByteArray, pad: Int = 16): ByteArray {
    if (src.size % pad == 0) return src
    val targetSize = (src.size / pad + 1) * pad
    val dest = ByteArray(targetSize)
    arrayCopy(src = src, dest = dest)
    return dest
}

fun mapValue(value: Float, fromStart: Float, frontEnd: Float, toStart: Float, toEnd: Float): Float {
    return (value - fromStart) / (frontEnd - fromStart) * (toEnd - toStart) + toStart
}

fun readableFileSize(size: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    if (size <= 0) return "0 B"
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble()))
            .replace(",", ".") + " " + units[digitGroups]
}

fun getJarPath(caller: Class<*>): String {
    val url = caller.protectionDomain.codeSource.location
    var path = URLDecoder.decode(url.file, "UTF-8")

    // remove jar name from path
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
        path = path.substring(1, path.lastIndexOf('/')) // cut first '/' for Windows
    } else {
        path = path.substring(0, path.lastIndexOf('/'))
    }

    if (path.endsWith("target/classes")) { //launched from ide, remove classes from path
        path = path.substring(0, path.length - "/classes".length)
    }

    path = path.replace("/", File.separator)
    return path + File.separator
}
