package com.kotcrab.fate.nanoha.file

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.tex.ColorPalette
import com.kotcrab.fate.tex.ImageWriter
import com.kotcrab.fate.tex.Swizzling
import com.kotcrab.fate.util.toUnsignedInt
import java.io.File

/** @author Kotcrab */
@Suppress("UNUSED_VARIABLE")
class Tim2File(bytes: ByteArray) {
    private lateinit var image: ImageWriter
    private var width = 0
    private var height = 0

    constructor(file: File) : this(file.readBytes())

    init {
        with(FateInputStream(bytes)) {
            if (readStringAndTrim(4) != "TIM2") error("Not A TIM2 file")
            readInt()
            readInt()
            readInt()

            //0x10
            val fileLen = readInt()
            readInt()
            readInt()
            val widthOld = readShort()
            val heightOld = readShort()

            //0x20
            readByte()
            readByte()
            readByte()
            val mode = readByte().toUnsignedInt()
            if (mode !in arrayOf(4, 5)) error("Unsupported mode")
            width = readShort().toUnsignedInt()
            height = readShort().toUnsignedInt()
            readInt()
            readInt()

            //0x30
            readInt()
            readInt()
            readInt()
            readInt()

            if (readStringAndTrim(4) != "SFCG") error("Expected SFCG header")
            val pageCount = readInt()
            val unkPadding = readInt()
            readShort()
            readShort()
            if (pageCount != 1) error("Expected exactly one page")

            //sfcg 0x10
            readShort()
            readShort()
            readInt()
            readInt()
            readInt()

            //sfcg 0x20
            skip(0x10)

            skip(unkPadding)

            if (mode == 4) { //4 bpp
                val pixelDataSize = width * height / 2
                val swizzledBytes = readBytes(pixelDataSize)
                val unswizzledBytes = if (pixelDataSize > 0x100) {
                    Swizzling.unswizzle4BPP(swizzledBytes, width, height)
                } else {
                    println("WARN: Not enough data to unswizzle, skipping")
                    swizzledBytes
                }
                val palette = ColorPalette(readBytes(4 * 16), ColorPalette.Mode.RGBA8888)

                val texStream = FateInputStream(unswizzledBytes)
                image = ImageWriter(width, height)
                repeat(pixelDataSize) {
                    val data = texStream.readByte().toUnsignedInt()
                    val pixel1 = data and 0b11110000 shr 4
                    val pixel2 = data and 0b00001111
                    image.writePixel(palette[pixel2])
                    image.writePixel(palette[pixel1])
                }
                texStream.close()
            } else if (mode == 5) { //5 bpp
                val pixelDataSize = width * height
                val swizzledBytes = readBytes(pixelDataSize)
                val unswizzledBytes = Swizzling.unswizzle8BPP(swizzledBytes, width, height)
                val palette = ColorPalette(readBytes(4 * 256), ColorPalette.Mode.RGBA8888)

                val texStream = FateInputStream(unswizzledBytes)
                image = ImageWriter(width, height)
                repeat(pixelDataSize) {
                    val pixel = texStream.readByte().toUnsignedInt()
                    image.writePixel(palette[pixel])
                }
                texStream.close()
            }
        }
    }

    fun writeToPng(file: File) {
        image.writeToPng(file)
    }
}
