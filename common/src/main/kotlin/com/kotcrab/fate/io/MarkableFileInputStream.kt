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
