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

package com.kotcrab.fate.nanoha.patcher

import com.kotcrab.fate.io.LERandomAccessFile
import com.kotcrab.fate.io.SequentialArrayReader
import com.kotcrab.fate.io.SequentialArrayWriter
import com.kotcrab.fate.tex.ColorPalette
import com.kotcrab.fate.tex.ImageReader
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/** @author Kotcrab */
class Tm2NamesPatcher(private val srcTm2: File, private val srcPng: File) {
    fun patchTo(outFile: File) {
        srcTm2.copyTo(outFile, overwrite = true)
        val img = ImageIO.read(srcPng)

        with(LERandomAccessFile(outFile, "rw")) {
            seek(0x8470)
            val origPaletteBytes = ByteArray(0x400)
            read(origPaletteBytes)
            val palette = ColorPalette(origPaletteBytes, ColorPalette.Mode.RGBA8888)

            seek(0x24)
            writeShort(img.width)
            writeShort(img.height)

            // clear old data
            seek(0x470)
            write(ByteArray(256 * 256 / 2))

            seek(0x470)
            write(encodeNewPixelData(img, palette))
            write(origPaletteBytes)

            seek(0x10)
            writeInt(length().toInt() - 0x10)

            close()
        }
    }

    private fun encodeNewPixelData(img: BufferedImage, palette: ColorPalette): ByteArray {
        val newTexDataSize = img.width * img.height / 2
        val newTexBytesStream = ByteArrayOutputStream()
        val newImageReader = ImageReader(img, img.width, img.height)
        repeat(newTexDataSize) {
            fun correctAlpha(color: Int): Int {
                val alphaThreshold = 96
                if (color ushr 24 < alphaThreshold) return 0
                return color
            }

            val color2 = correctAlpha(newImageReader.nextPixel())
            val color1 = correctAlpha(newImageReader.nextPixel())
            val colorId2 = palette.getClosestColor(color2)
            val colorId1 = palette.getClosestColor(color1)
            val encoded = (colorId1 shl 4) or colorId2
            newTexBytesStream.write(encoded)
        }
        return swizzle4Bpi(newTexBytesStream.toByteArray(), img.width, img.height)
    }

    private fun swizzle4Bpi(encodedBytes: ByteArray, width: Int, height: Int): ByteArray {
        val reader = SequentialArrayReader(encodedBytes)
        // all units in bytes, no pixel units here
        val blockWidth = 16
        val blockHeight = 8

        val blocksPerLine = width / blockWidth
        val blockVerticalLines = height / blockHeight
        val blocks = Array(blocksPerLine * blockVerticalLines / 2, { SequentialArrayReader(reader.read(blockWidth * blockHeight)) })

        val output = SequentialArrayWriter(ByteArray(width * height / 2))
        //this will probably only work for texture 256 pixels wide but the general approach didn't work so whatever
        repeat(blockVerticalLines) { blockLine ->
            repeat(blockHeight) {
                repeat(8) { blockId ->
                    repeat(blockWidth) {
                        output.write(blocks[blockLine * 8 + blockId].read())
                    }
                }
            }
        }

        return output.bytes
    }
}
