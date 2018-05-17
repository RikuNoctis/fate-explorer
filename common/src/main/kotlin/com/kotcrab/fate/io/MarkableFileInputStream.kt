package com.kotcrab.fate.io

import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.nio.channels.FileChannel

/** @author Kotcrab */
class MarkableFileInputStream(fis: FileInputStream) : FilterInputStream(fis) {
    private val fileChannel: FileChannel = fis.channel
    private var mark: Long = -1L

    override fun markSupported(): Boolean {
        return true
    }

    @Synchronized
    override fun mark(readLimit: Int) {
        mark = try {
            fileChannel.position()
        } catch (ex: IOException) {
            -1L
        }
    }

    @Synchronized
    @Throws(IOException::class)
    override fun reset() {
        if (mark == -1L) {
            throw IOException("not marked")
        }
        fileChannel.position(mark)
    }
}
