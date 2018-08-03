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

package com.kotcrab.fate.patcher.extra.file

import com.kotcrab.fate.io.*
import com.kotcrab.fate.patcher.extra.ExtraTranslation
import com.kotcrab.fate.util.WINDOWS_932
import com.kotcrab.fate.util.getSubArrayPos
import com.kotcrab.fate.util.toHex
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraChrDatFilePatcher(origBytes: ByteArray, outFile: File, translation: ExtraTranslation, origTranslation: ExtraTranslation,
                             translationOffset: Int, count: Int, charset: Charset = Charsets.WINDOWS_932) {
    init {
        outFile.writeBytes(origBytes)
        val raf = LERandomAccessFile(outFile)

        val attachedTextMap = mutableMapOf<String, Long>()
        repeat(count) { offset ->
            val en = translation.getTranslation(offset, translationOffset)
            val origEn = origTranslation.getTranslation(offset, translationOffset)
            if (en != origEn) {
                val enByteLen = FateOutputStream(ByteArrayOutputStream()).writeDatString(en, charset = charset)
                val origByteLen = FateOutputStream(ByteArrayOutputStream()).writeDatString(origEn, charset = charset)
                if (enByteLen <= origByteLen) {
                    raf.seek(getSubArrayPos(origBytes, origEn.toByteArray()).toLong())
                    raf.writeDatString(en, charset = charset)
                } else {
                    val pos = getSubArrayPos(origBytes, origEn.toByteArray())
                    if (pos == -1) error("$origEn not found")
                    var written = false
                    with(FateInputStream(origBytes)) {
                        while (!eof()) {
                            val pointerPos = count()
                            if (readInt() == pos) {
                                var attachedPointer = attachedTextMap[en]
                                if (attachedPointer == null) {
                                    attachedPointer = raf.length()
                                    raf.seek(raf.length())
                                    raf.writeDatString(en, charset = charset)
                                    attachedTextMap[en] = attachedPointer
                                }
                                raf.seek(pointerPos.toLong())
                                raf.writeInt(attachedPointer.toInt())
                                if (written) {
                                    println("TN WARN: Written in-dungeon twice: at ${pointerPos.toHex()}, $en")
                                }
                                written = true
                            }
                        }
                    }
                    if (written == false) {
                        println("$en not written")
                    }
                }
            }
        }
        raf.seek(raf.length())
        raf.align(16)
        raf.close()
    }
}
