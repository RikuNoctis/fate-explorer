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

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** @author Kotcrab */
class FontLoader {
    private val fonts = mutableMapOf<Int, BufferedImage>()
    private val glpyhCounts = mutableMapOf<Int, Int>()

    private val glyphWidth = 16
    private val glyphHeight = 20

    fun load(fontDir: File) {
        fontDir.listFiles().forEach { file ->
            val fontId = file.nameWithoutExtension.replace("story", "").toInt()
            fonts[fontId] = ImageIO.read(file)
        }
        fonts.forEach { chapter, img ->
            glpyhCounts[chapter] = countGlyphs(img)
        }
    }

    private fun countGlyphs(img: BufferedImage): Int {
        var allowedBlanks = 2
        val glyphRows = img.width / glyphWidth
        val glyphColumns = img.height / glyphHeight

        repeat(glyphRows) { row ->
            repeat(glyphColumns) { column ->
                val glyphId = row * glyphRows + column
                val glyphImg = img.getSubimage(column * glyphWidth, row * glyphHeight, glyphWidth, glyphHeight)
                if (glyphImg.isBlank()) {
                    allowedBlanks--
                    if (allowedBlanks < 0) {
                        return glyphId
                    }
                }
            }
        }
        error("Font did not end with blank glyphs")
    }

    fun getGlyphCount(chapter: Int): Int {
        return glpyhCounts[chapter]!!
    }

    fun getGlyph(chapter: Int, glyphId: Int): BufferedImage {
        val img = fonts[chapter]!!
        val glyphRows = img.width / glyphWidth
        val row = glyphId / glyphRows
        val column = glyphId % glyphRows
        return img.getSubimage(column * glyphWidth, row * glyphHeight, glyphWidth, glyphHeight)
    }

    fun getCount(): Int {
        return fonts.size
    }

    private fun BufferedImage.isBlank(): Boolean {
        val buf = getRGB(0, 0, width, height, null, 0, width)
        return buf.all { it ushr 24 == 0 }
    }
}
