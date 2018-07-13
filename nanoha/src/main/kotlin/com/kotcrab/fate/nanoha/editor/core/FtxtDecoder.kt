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

package com.kotcrab.fate.nanoha.editor.core

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.util.getSubArrayPos
import com.kotcrab.fate.util.toUnsignedInt
import java.io.File

/** @author Kotcrab */
@Suppress("UNUSED_VARIABLE")
class FtxtDecoder(asm: ByteArray) {
    constructor(asm: File) : this(asm.readBytes())

    val texts: Array<IntArray>

    init {
        val ftxtSection = getSubArrayPos(asm, "FTXT".toByteArray())
        if (ftxtSection == -1) error("This ASM file does not have FTXT section")
        val texts = mutableListOf<IntArray>()
        with(FateInputStream(asm)) {
            setPos(ftxtSection)
            if (readString(4) != "FTXT") error("Not a FTXT section")
            val unkFlag = readInt()
            val count = readInt()
            val unkPadding = readInt()

            val headerSize = readInt()
            val nameSectionSize = readInt()
            val pointerSectionSize = readInt()
            val textSectionSize = readInt()

            val nameSectionOffset = readInt()
            val pointerSectionOffset = readInt()
            val textSectionOffset = readInt()
            val unkPadding2 = readInt()

            val fileName = readDatString()
            align(16)
            val textSectionStart = count() + pointerSectionSize

            val pointers = mutableListOf<Int>()
            repeat(count) {
                pointers.add(readInt())
            }

            pointers.forEach { pointer ->
                if (pointer != -1) {
                    setPos(textSectionStart + pointer)
                    val codePoints = mutableListOf<Int>()
                    while (true) {
                        val codePoint = readShort().toUnsignedInt()
                        if (codePoint == 0xFFFF) break
                        codePoints.add(codePoint)
                    }
                    texts.add(codePoints.toIntArray())
                }
            }
            close()
        }
        this.texts = texts.toTypedArray()
    }
}
