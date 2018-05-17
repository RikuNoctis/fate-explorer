package com.kotcrab.fate.tex

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** @author Kotcrab */
class ImageWriter(val width: Int, val height: Int, private val removeAlphaMask: Boolean, private val argbAlphaMask: Int) {
    constructor(width: Int, height: Int) : this(width, height, false, -1)

    private val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    private var xPos = 0
    private var yPos = 0

    fun writePixel(argbColor: Int) {
        if (removeAlphaMask && argbColor == argbAlphaMask) {
            img.setRGB(xPos, yPos, 0)
        } else {
            img.setRGB(xPos, yPos, argbColor)
        }
        xPos++
        if (xPos >= width) {
            yPos++
            xPos = 0
        }
    }

    fun eof(): Boolean {
        return xPos == width && yPos == height
    }

    fun writeToPng(path: String, outWidth: Int = width, outHeight: Int = height) {
        writeToPng(File(path), outWidth, outHeight)
    }

    fun writeToPng(file: File, outWidth: Int = width, outHeight: Int = height) {
        val outImg = if (outWidth == width && outHeight == height) {
            img
        } else {
            img.getSubimage(0, 0, Math.min(img.width, outWidth), Math.min(img.height, outHeight))
        }
        ImageIO.write(outImg, "PNG", file)
    }
}
