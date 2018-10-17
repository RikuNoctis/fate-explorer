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

import kio.LERandomAccessFile
import kio.util.seek
import java.io.File

/** @author Kotcrab */
class SprNamesPatcher(private val sprFile: File, private val boundsDef: File, private val newWidth: Int, private val newHeight: Int) {
    fun patchTo(outFile: File) {
        sprFile.copyTo(outFile, overwrite = true)
        with(LERandomAccessFile(outFile)) {
            seek(0x24)
            val pointers = mutableListOf<Int>()
            while (true) {
                val ptr = readInt()
                if (ptr == -1) break
                pointers.add(ptr)
            }
            val entryPointers = pointers.associateBy({
                seek(it + 0x14)
                readInt()
            }, { it })

            //DEBUG - test single entry only
//            seek(0x24)
//            repeat(pointers.size) {
//                writeInt(entryPointers[82]!!)
//            }

            val lines = mutableListOf<MutableList<String>>()
            boundsDef.readLines().forEach {
                if (it.isBlank()) return@forEach
                val parts = it.split(" ")
                if (parts.size < 5) return@forEach
                if (it.endsWith("^")) {
                    lines.last().add(it)
                } else {
                    lines.add(mutableListOf(it))
                }
            }

            lines.forEach {
                val masterId = it[0].split(" ")[0].toInt()
                val entryBase = entryPointers[masterId]!!
                seek(entryBase)
                writeInt(it.size) //draw calls

                it.forEachIndexed { drawCallIdx, str ->
                    val parts = str.split(" ")
                    val id = parts[0].toInt()
                    val x = parts[1].toInt()
                    val y = parts[2].toInt()
                    val width = parts[3].toInt()
                    val height = parts[4].toInt()

                    val horizontalSampleStart = x.toFloat() / newWidth
                    val horizontalSampleEnd = (x + width).toFloat() / newWidth
                    val verticalSampleStart = y.toFloat() / newHeight
                    val verticalSampleEnd = (y + height).toFloat() / newHeight
                    var horizontalRightBound = (width / 2).toFloat()
                    var horizontalLeftBound = -horizontalRightBound
                    var verticalTopBound = (height / 2).toFloat()
                    var verticalBottomBound = -verticalTopBound

                    fun offsetX(n: Int) {
                        horizontalRightBound += n
                        horizontalLeftBound += n
                    }

                    fun offsetY(n: Int) {
                        verticalBottomBound += n
                        verticalTopBound += n
                    }

                    if (height == 16) {
                        offsetY(2)
                    }

                    // combination id 34 Hayate & Reinforce
                    run {
                        if (id == 34) {
                            offsetX(-44)
                        }
                        if (id == 35) {
                            offsetX(-9)
                            offsetY(1)
                        }
                        if (id == 36) {
                            offsetX(36)
                        }
                    }

                    // combination id 37 Nanoha, Fate & Hayate
                    run {
                        val off = 3
                        if (id == 37) {
                            offsetX(-60 + off)
                        }
                        if (id == 38) {
                            offsetX(-28 + off)
                            offsetY(-3)
                        }
                        if (id == 39) {
                            offsetX(-5 + off)
                        }
                        if (id == 40) {
                            offsetX(22 + off)
                            offsetY(1)
                        }
                        if (id == 41) {
                            offsetX(59 + off)
                        }
                    }

                    // combination id 42 Thoma, Lily & Isis
                    run {
                        val off = 20
                        if (id == 42) {
                            offsetX(-60 + off)
                        }
                        if (id == 43) {
                            offsetX(-31 + off)
                            offsetY(-3)
                        }
                        if (id == 44) {
                            offsetX(-13 + off)
                        }
                        if (id == 45) {
                            offsetX(9 + off)
                            offsetY(1)
                        }
                        if (id == 46) {
                            offsetX(32 + off)
                        }
                    }

                    // combination id 50 Dark Fragment Rynith
                    run {
                        val off = -24
                        if (id == 50) {
                            offsetX(off)
                        }
                        if (id == 51) {
                            offsetX(84 + off)
                        }
                    }

                    // combination id 52 Dark Fragment Presea
                    run {
                        val off = -28
                        if (id == 52) {
                            offsetX(off)
                        }
                        if (id == 53) {
                            offsetX(88 + off)
                        }
                    }

                    // combination id 54 Dark Fragment Thoma
                    run {
                        val off = -27
                        if (id == 54) {
                            offsetX(off)
                        }
                        if (id == 55) {
                            offsetX(86 + off)
                        }
                    }

                    // combination id 56 Dark Fragment Lily
                    run {
                        val off = -15
                        if (id == 56) {
                            offsetX(off)
                        }
                        if (id == 57) {
                            offsetX(74 + off)
                        }
                    }

                    // combination id 58 Dark Fragment Einhard
                    run {
                        val off = -31
                        if (id == 58) {
                            offsetX(off)
                        }
                        if (id == 59) {
                            offsetX(90 + off)
                        }
                    }

                    // combination id 60 Dark Fragment Yunno
                    run {
                        val off = -25
                        if (id == 60) {
                            offsetX(off)
                        }
                        if (id == 61) {
                            offsetX(84 + off)
                        }
                    }

                    // combination id 62 Dark Fragment Stern
                    run {
                        val off = -22
                        if (id == 62) {
                            offsetX(off)
                        }
                        if (id == 63) {
                            offsetX(81 + off)
                        }
                    }

                    // combination id 64 Dark Fragment Vivio
                    run {
                        val off = -20
                        if (id == 64) {
                            offsetX(off)
                        }
                        if (id == 65) {
                            offsetX(79 + off)
                        }
                    }

                    // combination id 66 Dark Fragment Levi
                    run {
                        val off = -19
                        if (id == 66) {
                            offsetX(off)
                        }
                        if (id == 67) {
                            offsetX(76 + off)
                        }
                    }

                    // combination id 68 Dark Fragment Dearche
                    run {
                        val off = -34
                        if (id == 68) {
                            offsetX(off)
                        }
                        if (id == 69) {
                            offsetX(94 + off)
                        }
                    }

                    // combination id 70 Dark Fragment Fate
                    run {
                        val off = -18
                        if (id == 70) {
                            offsetX(off)
                        }
                        if (id == 71) {
                            offsetX(78 + off)
                        }
                    }

                    // combination id 72 Amita & Hayate
                    run {
                        val off = 7
                        if (id == 72) {
                            offsetX(-44 + off)
                        }
                        if (id == 73) {
                            offsetX(-12 + off)
                            offsetY(1)
                        }
                        if (id == 74) {
                            offsetX(26 + off)
                        }
                    }

                    // combination id 76 Thoma & Lily
                    run {
                        val off = 18
                        if (id == 76) {
                            offsetX(-44 + off)
                        }
                        if (id == 77) {
                            offsetX(-8 + off)
                            offsetY(1)
                        }
                        if (id == 78) {
                            offsetX(16 + off)
                        }
                    }

                    // combination id 79 Nanoha & Vivio
                    run {
                        val off = 14
                        if (id == 79) {
                            offsetX(-45 + off)
                        }
                        if (id == 80) {
                            offsetX(-4 + off)
                            offsetY(1)
                        }
                        if (id == 81) {
                            offsetX(24 + off)
                        }
                    }

                    // combination id 82 Amita & Kyrie
                    run {
                        val off = 5
                        if (id == 82) {
                            offsetX(-37 + off)
                        }
                        if (id == 83) {
                            offsetX(-5 + off)
                            offsetY(1)
                        }
                        if (id == 84) {
                            offsetX(24 + off)
                        }
                    }


                    val drawCall = entryBase + 0x8 + drawCallIdx * 0x60

                    seek(drawCall + 0xC)
                    writeInt(id)
                    seek(drawCall + 0x10)
                    writeFloat(horizontalSampleStart)
                    seek(drawCall + 0x14)
                    writeFloat(verticalSampleStart)

                    seek(drawCall + 0x18)
                    writeFloat(horizontalLeftBound)
                    seek(drawCall + 0x1C)
                    writeFloat(verticalTopBound)
                    seek(drawCall + 0x24)
                    writeFloat(horizontalSampleStart)

                    seek(drawCall + 0x28)
                    writeFloat(verticalSampleEnd)
                    seek(drawCall + 0x2C)
                    writeFloat(horizontalLeftBound)
                    seek(drawCall + 0x30)
                    writeFloat(verticalBottomBound)

                    seek(drawCall + 0x38)
                    writeFloat(horizontalSampleEnd)
                    seek(drawCall + 0x3C)
                    writeFloat(verticalSampleStart)
                    seek(drawCall + 0x40)
                    writeFloat(horizontalRightBound)
                    seek(drawCall + 0x44)
                    writeFloat(verticalTopBound)

                    seek(drawCall + 0x4C)
                    writeFloat(horizontalSampleEnd)
                    seek(drawCall + 0x50)
                    writeFloat(verticalSampleEnd)
                    seek(drawCall + 0x54)
                    writeFloat(horizontalRightBound)

                    seek(drawCall + 0x58)
                    writeFloat(verticalBottomBound)
                }
            }
            close()
        }
    }
}
