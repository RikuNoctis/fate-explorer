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

import kio.KioInputStream
import kio.util.child
import java.io.File
import javax.imageio.ImageIO

/** @author Kotcrab */
@Suppress("UNUSED_VARIABLE")
class SprSplitter(private val sprFile: File, private val pngFile: File) {
    fun splitTo(outDir: File) {
        val img = ImageIO.read(pngFile)

        with(KioInputStream(sprFile)) {
            setPos(0x24)
            while (true) {
                val ptr = readInt()
                if (ptr == -1) break
                temporaryJump(ptr) {
                    //0x00
                    val drawCalls = readInt()
                    val ptr2 = readInt()

                    repeat(drawCalls) { drawCall ->
                        val un1 = readInt()
                        val un2 = readInt()

                        //0x10
                        val un3 = readInt()
                        val id = readInt()
                        val horizontalSampleStart = readFloat()
                        val verticalSampleStart = readFloat()

                        //0x20
                        val horizontalLeftBoundPx = readFloat()
                        val verticalTopBoundPx = readFloat()
                        val f2 = readFloat()
                        val horizontalSampleStart2 = readFloat()

                        //0x30
                        val verticalSampleEnd = readFloat()
                        val horizontalLeftBound2Px = readFloat()
                        val verticalBottomBoundPx = readFloat()
                        val f3 = readFloat()

                        //0x40
                        val horizontalSampleEnd = readFloat()
                        val verticalSampleStart2 = readFloat()
                        val horizontalRightBoundPx = readFloat()
                        val verticalTopBound2Px = readFloat()

                        //0x50
                        val f6 = readFloat()
                        val horizontalSampleEnd2 = readFloat()
                        val verticalSampleEnd2 = readFloat()
                        val horizontalRightBound2Px = readFloat()

                        //0x60
                        val verticalBottomBound2Px = readFloat()
                        val f8 = readFloat()

                        val outImg = if (drawCall == 0) outDir.child("$id.png") else outDir.child("${id}_$drawCall.png")
                        val x = (horizontalSampleStart * img.width).toInt()
                        val y = (verticalSampleStart * img.height).toInt()
                        val width =
                            (horizontalSampleEnd * img.width).toInt() - (horizontalSampleStart * img.width).toInt()
                        val height =
                            (verticalSampleEnd * img.height).toInt() - (verticalSampleStart * img.height).toInt()
                        ImageIO.write(img.getSubimage(x, y, width, height), "PNG", outImg)
                    }
                }
            }
        }
    }
}
