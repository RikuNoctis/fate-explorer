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

package com.kotcrab.fate.tex

import java.awt.image.BufferedImage

/** @author Kotcrab */
class ImageReader(srcImage: BufferedImage, newImageDataWidth: Int, newImageDataHeight: Int) {
    private val image = BufferedImage(newImageDataWidth, newImageDataHeight, srcImage.type)
    private var xPos = 0
    private var yPos = 0

    init {
        val g2d = image.createGraphics()
        g2d.drawImage(srcImage, 0, 0, null)
        g2d.dispose()
    }

    fun nextPixel(): Int {
        val pixel = image.getRGB(xPos, yPos)
        xPos++
        if (xPos >= image.width) {
            yPos++
            xPos = 0
        }
        return pixel
    }
}
