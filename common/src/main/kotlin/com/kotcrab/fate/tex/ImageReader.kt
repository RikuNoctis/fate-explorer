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
