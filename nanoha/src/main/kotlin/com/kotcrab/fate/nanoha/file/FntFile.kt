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

package com.kotcrab.fate.nanoha.file

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.tex.ImageWriter
import com.kotcrab.fate.tex.Swizzling
import com.kotcrab.fate.util.toUnsignedInt
import java.io.File

/** @author Kotcrab */
@Suppress("UNUSED_VARIABLE")
class FntFile(bytes: ByteArray) {
    constructor(file: File) : this(file.readBytes())

    private lateinit var image: ImageWriter

    init {
        with(FateInputStream(bytes)) {
            if (readStringAndTrim(4) != "FONT") error("Not a FONT file")
            val xadvanceSection = readInt()
            val paletteSection = readInt()
            val texDataSection = readInt()

            val headerSize = readInt() //60 00 00 00 //maybe
            val xadvanceSectionSize = readInt()
            val paletteSize = readInt()
            val texDataSize = readInt()

            //4C 02 00 00 | 20 00 00 00 | 13 00 00 00 | 00 00 00 00
            val width = readInt()
            val height = readInt()
            readInt()
            readInt()

            //0F 00 00 00 | 13 00 00 00 | 10 00 00 00 | 14 00 00 00
            readInt()
            readInt()
            readInt()
            readInt()

            //00 02 00 00 | 80 01 00 00 | 00 89 01 00 | A0 0C 00 00
            readInt()
            readInt()
            val tim2Section = readInt()
            val tim2Size = readInt()

            //all zeros
            repeat(4) {
                readInt()
            }

            setPos(paletteSection)
            //val palette = ColorPalette(readBytes(paletteSize), ColorPalette.Mode.RGBA8888) //8 bpp

            setPos(texDataSection)
            val dataWidth = 512
            val swizzledBytes = readBytes(texDataSize)
            val unswizzledBytes = Swizzling.unswizzle4BPP(swizzledBytes, dataWidth, 600)
            val texStream = FateInputStream(unswizzledBytes)
            image = ImageWriter(dataWidth, 600)
            repeat(texDataSize) {
                val data = texStream.readByte().toUnsignedInt()
                val alpha1 = data and 0b11110000 shr 4
                val alpha2 = data and 0b00001111
                val color1 = 0xFFFFFF or (alpha1 * 17 shl 24)
                val color2 = 0xFFFFFF or (alpha2 * 17 shl 24)
                image.writePixel(color2)
                image.writePixel(color1)
            }
            texStream.close()
            close()
        }
    }

    fun writeToPng(file: File) {
        image.writeToPng(file)
    }
}
