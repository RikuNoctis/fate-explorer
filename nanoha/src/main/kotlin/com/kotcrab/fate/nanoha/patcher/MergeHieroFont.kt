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

import com.kotcrab.fate.file.HieroFile
import com.kotcrab.fate.nanoha.file.FntFile
import com.kotcrab.fate.tex.ImageReader
import kio.KioInputStream
import kio.LERandomAccessFile
import kio.SequentialArrayReader
import kio.SequentialArrayWriter
import kio.util.child
import kio.util.seek
import kio.util.toWHex
import java.awt.Color
import java.awt.Graphics2D
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/** @author Kotcrab */
class MergeHieroFont(private val hieroDir: File, fontDir: File) {
    private val charIdToGlyphIdMap = mutableMapOf<Int, Int>()

    init {
        val hiero = HieroFile(hieroDir.child("font.fnt"), hieroDir.child("font.png"))
        writeNewFontPng(hiero)
        val newFont = fontDir.child("story01_new.fnt")
        GearsFntMerge(fontDir.child("story01.fnt"), hieroDir.child("new.png"), hiero)
                .mergeTo(newFont, charIdToGlyphIdMap)
        FntFile(newFont).writeToPng(newFont.resolveSibling("story01_new.png"))
    }

    private fun writeNewFontPng(hiero: HieroFile) {
        val font = ImageIO.read(hieroDir.child("stock.png"))

        val glyphWidth = 16
        val glyphHeight = 20
        val glyphRows = font.width / glyphWidth

        val graphics = font.graphics as Graphics2D
        graphics.background = Color(0x00FFFFFF, true)
        graphics.clearRect(0, 20, 512, 100)
        hiero.chars.forEach {
            val row: Int
            val column: Int
            var warnBleed = true
            when {
                it.id in arrayOf(0, 10, 127) -> {
                    return@forEach
                }
                it.id == 32 -> {
                    charIdToGlyphIdMap[it.id] = it.id
                    return@forEach
                }
                it.id == 8213 -> { //―
                    row = 5
                    column = 0
                }
                it.id == 8217 -> { //’
                    row = 5
                    column = 1
                }
                it.id == 8220 -> { //“
                    row = 5
                    column = 2
                }
                it.id == 8221 -> { //”
                    row = 5
                    column = 3
                }
                it.id == 8230 -> { //…
                    row = 5
                    column = 4
                }
                it.id == 64 -> { //@
                    row = 4
                    column = 0
                    warnBleed = false
                }
                it.id == 87 -> { //W
                    row = 4
                    column = 2
                    warnBleed = false
                }
                it.id == 228 -> { //ä
                    row = 4
                    column = 4
                }
                it.id == 231 -> { //ç
                    row = 4
                    column = 5
                }
                it.id == 252 -> { //ü
                    row = 4
                    column = 6
                }
                it.id == 8211 -> { //–
                    row = 4
                    column = 7
                }
                it.id == 8216 -> { //‘
                    row = 4
                    column = 8
                }
                it.id in 33..126 -> { //normal ascii
                    row = it.id / glyphRows
                    column = it.id % glyphRows
                }
                else -> error("Don't know where to write char ID ${it.id}")
            }

            val glyphId = row * glyphRows + column
            charIdToGlyphIdMap[it.id] = row * glyphRows + column
            if (glyphId != it.id) {
                println("Remapped char ID to glyph ID: ${it.id} -> $glyphId (0x${glyphId.toWHex()})")
            }
            val targetX = column * glyphWidth
            var targetY = row * glyphHeight + it.yoffset
            if (it.id == 126) targetY += 3
            if (warnBleed && (it.width > 16 || it.height > 20)) println("WARN: Char ID ${it.id} may bleed.")
            graphics.drawImage(hiero.glyphs[it.id], targetX, targetY, null)
        }
        graphics.drawImage(ImageIO.read(hieroDir.child("musicnote.png")), 5 * glyphWidth, 5 * glyphHeight + 3, null)
        graphics.drawImage(ImageIO.read(hieroDir.child("larrow.png")), 6 * glyphWidth, 5 * glyphHeight + 6, null)
        graphics.drawImage(ImageIO.read(hieroDir.child("lbracket.png")), 7 * glyphWidth + 3, 5 * glyphHeight + 1, null)
        graphics.drawImage(ImageIO.read(hieroDir.child("rbracket.png")), 8 * glyphWidth, 5 * glyphHeight + 4, null)
        graphics.drawImage(ImageIO.read(hieroDir.child("star.png")), 9 * glyphWidth, 5 * glyphHeight + 2, null)

        graphics.dispose()
        ImageIO.write(font, "PNG", hieroDir.child("new.png"))
    }
}

private class GearsFntMerge(val srcFnt: File, val pngFile: File, val hieroDefs: HieroFile) {
    private lateinit var newBytes: ByteArray
    private var glyphWidthSectPos: Int = 0
    private var texDataPos: Int = 0
    private var texDataSize: Int = 0

    init {
        with(KioInputStream(srcFnt)) {
            skip(0x4)
            glyphWidthSectPos = readInt()
            skip(0x4)
            texDataPos = readInt()
            skip(0xC)
            texDataSize = readInt()
            val pixels = texDataSize * 2
            val width = 512
            val height = pixels / width

            val newImage = ImageIO.read(pngFile)
            val newImageReader = ImageReader(newImage, newImage.width, newImage.height)
            val newTexBytesStream = ByteArrayOutputStream()
            repeat(texDataSize) {
                val color1 = newImageReader.nextPixel()
                val color2 = newImageReader.nextPixel()
                val alpha1 = (color1 ushr 24 and 0xFF) / 17
                val alpha2 = (color2 ushr 24 and 0xFF) / 17
                val encoded = (alpha2 shl 4) or alpha1
                newTexBytesStream.write(encoded)
            }
            newBytes = swizzle4Bpp(newTexBytesStream.toByteArray(), width, height)
            if (newBytes.size > texDataSize) error("New bytes size greater than stock, can't merge")
        }
    }

    private fun swizzle4Bpp(encodedBytes: ByteArray, width: Int, height: Int): ByteArray {

        val chunksPerLine = width / 32
        val chunkLines = height / 8
        val chunks = mutableListOf<ByteArray>()
        val reader = SequentialArrayReader(encodedBytes)

        repeat(chunkLines) {
            val newChunks = Array(chunksPerLine, {
                val chunk = ByteArray(16 * 8)
                chunks.add(chunk)
                SequentialArrayWriter(chunk)
            })

            repeat(8) {
                repeat(chunksPerLine) { lineChunkIdx ->
                    repeat(16) {
                        newChunks[lineChunkIdx].write(reader.read())
                    }
                }
            }
        }

        val output = ByteArrayOutputStream()
        chunks.forEach { output.write(it) }
        return output.toByteArray()
    }

    fun mergeTo(outFile: File, charIdToGlyphId: Map<Int, Int>) {
        srcFnt.copyTo(outFile, overwrite = true)
        with(LERandomAccessFile(outFile)) {
            hieroDefs.chars.forEach { char ->
                val glyphId = charIdToGlyphId[char.id] ?: return@forEach
                seek(glyphWidthSectPos + glyphId * 0x2)
                writeByte(char.width)
            }
            seek(glyphWidthSectPos + 165 * 0x2) //music note
            writeByte(10)
            seek(glyphWidthSectPos + 166 * 0x2) //left arrow
            writeByte(15)
            seek(glyphWidthSectPos + 167 * 0x2) //left jp bracket
            writeByte(10)
            seek(glyphWidthSectPos + 168 * 0x2) //right jp bracket
            writeByte(10)
            seek(glyphWidthSectPos + 169 * 0x2) //star
            writeByte(16)

            seek(texDataPos)
            write(ByteArray(texDataSize))
            seek(texDataPos)
            write(newBytes)

            val xadvSectPos = texDataPos + 0x7800
            val allocSize = 0x2000
            seek(xadvSectPos)
            write(ByteArray(allocSize))
            seek(xadvSectPos)
            hieroDefs.chars.forEach { char ->
                val glyphId = charIdToGlyphId[char.id] ?: return@forEach
                seek(xadvSectPos + glyphId * 0x2)
                writeByte(char.xadvance + 1)
            }
            seek(xadvSectPos + 165 * 0x2) //music note
            writeByte(13)
            seek(xadvSectPos + 166 * 0x2) //left arrow
            writeByte(16)
            seek(xadvSectPos + 167 * 0x2) //left jp bracket
            writeByte(10)
            seek(xadvSectPos + 168 * 0x2) //right jp bracket
            writeByte(7)
            seek(xadvSectPos + 169 * 0x2) //star
            writeByte(19)

            println("Xadv ended at ${filePointer.toWHex()}, used ${(filePointer - xadvSectPos).toWHex()} bytes up to this point")
            val xoffsetSectPos = texDataPos + 0x8000
            seek(xoffsetSectPos)
            hieroDefs.chars.forEach { char ->
                val glyphId = charIdToGlyphId[char.id] ?: return@forEach
                seek(xoffsetSectPos + glyphId * 0x2)
                writeByte(char.xoffset)
            }
            seek(xoffsetSectPos + 165 * 0x2) //music note
            writeByte(1)
            seek(xoffsetSectPos + 166 * 0x2) //left arrow
            writeByte(0)
            seek(xoffsetSectPos + 167 * 0x2) //left jp bracket
            writeByte(0)
            seek(xoffsetSectPos + 168 * 0x2) //right jp bracket
            writeByte(0)
            seek(xoffsetSectPos + 169 * 0x2) //star
            writeByte(0)

            println("Xoffset ended at ${filePointer.toWHex()}, used ${(filePointer - xadvSectPos).toWHex()} bytes up to this point")
            val kerningSectPos = texDataPos + 0x8400
            seek(kerningSectPos)
            val extraKerningCount = 1
            writeInt(hieroDefs.kernings.size + extraKerningCount)
            writeByte(0)
            writeByte(-3)
            writeByte(0xA0)
            writeByte(0xA0)
            hieroDefs.kernings.forEach {
                val firstGlyphId = charIdToGlyphId[it.first] ?: return@forEach
                val secondGlyphId = charIdToGlyphId[it.second] ?: return@forEach
                if (firstGlyphId > 0xFF || secondGlyphId > 0xFF || it.amount > 0xFF) error("Can't fit kerning data into single byte.")
                writeByte(0)
                writeByte(it.amount)
                writeByte(secondGlyphId)
                writeByte(firstGlyphId)
            }
            println("Kerning ended at ${filePointer.toWHex()}, used ${(filePointer - xadvSectPos).toWHex()} bytes up to this point")

            close()
        }
    }
}
