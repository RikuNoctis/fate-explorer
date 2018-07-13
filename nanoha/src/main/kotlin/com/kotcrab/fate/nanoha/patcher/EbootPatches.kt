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

import com.kotcrab.fate.util.*
import kmips.FpuReg.*
import kmips.Label
import kmips.Reg.*
import kmips.assembleAsHexString

/** @author Kotcrab */
class EbootPatches {
    private val funcs = Functions()
    private val vars = Variables()

    fun assembleExternalPatch(extPatchBase: Int) = assembleAsHexString(extPatchBase) {
        vars.ptrCurrentChar = word(0)
        repeat(4) { nop() }

        run {
            funcs.storePtrToCurrentChar = virtualPc
            sw(zero, 0x0, a1)
            val ctx = preserve(arrayOf(t7))
            lui(t7, vars.ptrCurrentChar.highBits())
            ori(t7, t7, vars.ptrCurrentChar.lowBits())
            sw(a2, 0x0, t7)
            ctx.restore()
            j(0x088F6D54)
            nop()
        }

        run {
            funcs.applyOffsets = virtualPc
            lui(a2, 0x3F80)

            val ctx = preserve(arrayOf(t0, t3, t4, t5, t7, t8, s0, s1, s2, s4, s6, s7))
            val xadvOffset = 0x7800 + 0x900 - 0x60
            val kerningOffset = 0x8400 + 0x900 - 0x60
            //t0 - stores offset for character
            lw(t7, 0x74, a0)
            lui(t8, xadvOffset.highBits())
            ori(t8, t8, xadvOffset.lowBits())
            addu(t7, t7, t8)
            addu(t7, t0, t7)
            lhu(t7, 0x0, t7) //load xadvance and store into f13
            mtc1(t7, f12)
            cvt.s.w(f12, f12)
            add.s(f13, f15, f12)

            //apply kerning pair data
            val kerningLoop = Label()
            val afterKerningLoop = Label()
            val secondKerningCheck = Label()
            val applyKerning = Label()
            lui(t3, vars.ptrCurrentChar.highBits())
            ori(t3, t3, vars.ptrCurrentChar.lowBits())
            lw(t3, 0x0, t3)
            lhu(t4, 0, t3)
            lhu(t5, 2, t3)
            li(t3, 0x0)
            ori(t3, t3, 0xFFFF)
            beq(t3, t5, afterKerningLoop)

            lui(t8, kerningOffset.highBits())
            ori(t8, t8, kerningOffset.lowBits())
            lw(t7, 0x74, a0)
            addu(s0, t7, t8)
            lw(s7, 0x0, s0) //load kerning pairs count
            li(s6, 0) //s6 loop counter
            label(kerningLoop)
            beq(s6, s7, afterKerningLoop)
            nop()
            addi(s0, s0, 0x4) //increment kerning ptr
            addi(s6, s6, 1) //increment loop counter
            lw(s2, 0, s0)
            srl(s2, s2, 24)
            beq(s2, t4, secondKerningCheck)
            nop()
            b(kerningLoop)
            nop()

            label(secondKerningCheck)
            lw(s2, 0, s0)
            srl(s2, s2, 16)
            andi(s2, s2, 0xFF)
            beq(s2, t5, applyKerning)
            nop()
            b(kerningLoop)
            nop()

            label(applyKerning)
            addi(s0, s0, 0x1)
            lb(s2, 0, s0)
            mtc1(s2, f12)
            cvt.s.w(f12, f12)
            add.s(f13, f13, f12)
            nop()

            label(afterKerningLoop)
            nop()

            //load cell width to be rendered
            lw(t7, 0x74, a0) //load offset into width data
            addu(t7, t0, t7) //get address for current character
            lhu(t0, 0x0, t7) //load character width and store into f12
            mtc1(t0, f12)
            cvt.s.w(f12, f12)

            ctx.restore()
            j(0x088F6D7C)
            nop()
        }

        run {
            funcs.applyXoffset = virtualPc
            lwc1(f12, 0x1C, a1)
            add.s(f12, f14, f12)

            val ctx = preserve(arrayOf(t0, t1, t8))
            lui(t8, vars.ptrCurrentChar.highBits())
            ori(t8, t8, vars.ptrCurrentChar.lowBits())
            lw(t8, 0x0, t8)
            lhu(t8, 0x0, t8)
            add(t8, t8, t8)
            val xoffsetOffset = 0x8000 + 0x900 - 0x60
            lw(t0, 0x74, a0)
            lui(t1, xoffsetOffset.highBits())
            ori(t1, t1, xoffsetOffset.lowBits())
            addu(t0, t0, t1)
            addu(t0, t0, t8)
            lb(t0, 0x0, t0) //load xoffset and store into f13
            mtc1(t0, f14)
            cvt.s.w(f14, f14)
            add.s(f12, f12, f14)

            ctx.restore()
            j(0x088F6F38)
            nop()
        }

        repeat(4) { nop() }
    }

    fun assembleEbootPatches(extPatchBase: Int): List<EbootPatch> {
        val patches = mutableListOf<EbootPatch>()
        with(patches) {
            patch("FontHax") {
                change(0x000F2D4C) {
                    j(funcs.storePtrToCurrentChar)
                }
                change(0x000F2D74) {
                    j(funcs.applyOffsets)
                }
                change(0x000F2F30) {
                    j(funcs.applyXoffset)
                    nop()
                }
                change(0x000F2D7C) {
                    nop() //nop old xadvance update, handled by ext patch
                }
            }
            patch("TitleTranslation") {
                change(0x00141E9C) {
                    val titleBytes = "Magical Girl Lyrical Nanoha A's PORTABLE ―THE GEARS OF DESTINY―"
                            .toByteArray(Charsets.UTF_8)
                    titleBytes.toList().chunked(4, transform = {
                        val b1 = it.getOrElse(0, { 0x0 }).toUnsignedInt() shl 24
                        val b2 = it.getOrElse(1, { 0x0 }).toUnsignedInt() shl 16
                        val b3 = it.getOrElse(2, { 0x0 }).toUnsignedInt() shl 8
                        val b4 = it.getOrElse(3, { 0x0 }).toUnsignedInt()
                        b1 or b2 or b3 or b4
                    }).forEach {
                        word(it.toLittleEndian())
                    }
                }
            }
        }
        return patches
    }

    class Functions {
        var storePtrToCurrentChar = 0
        var applyOffsets = 0
        var applyXoffset = 0
    }

    class Variables {
        var ptrCurrentChar = 0
    }
}
