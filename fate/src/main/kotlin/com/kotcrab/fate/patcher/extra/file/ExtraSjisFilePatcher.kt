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

import com.kotcrab.fate.file.SjisEntry
import com.kotcrab.fate.patcher.extra.ExtraTranslation
import kio.KioInputStream
import kio.KioOutputStream
import kio.util.WINDOWS_932
import java.io.EOFException
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraSjisFilePatcher(
    origBytes: ByteArray, origJpBytes: ByteArray,
    outFile: File, translation: ExtraTranslation, translationOffset: Int,
    charset: Charset = Charsets.WINDOWS_932
) {
    init {
        val out = KioOutputStream(outFile)
        var entryCount = 0
        val jpIn = KioInputStream(origJpBytes)
        with(KioInputStream(origBytes)) {
            while (!eof()) {
                try {
                    val l1 = readNullTerminatedString(charset)
                    val l2 = readNullTerminatedString(charset)
                    val l3 = readNullTerminatedString(charset)
                    val l4 = readNullTerminatedString(charset)
                    val l5 = readNullTerminatedString(charset)
                    val jpl1 = jpIn.readNullTerminatedString(charset)
                    val jpl2 = jpIn.readNullTerminatedString(charset)
                    val jpl3 = jpIn.readNullTerminatedString(charset)
                    val jpl4 = jpIn.readNullTerminatedString(charset)
                    val jpl5 = jpIn.readNullTerminatedString(charset)
                    val texts = arrayOf(l1, l2, l3, l4, l5)
                    if (texts.all { it.isNotBlank() }) {
                        if (SjisEntry(l1, l2, l3, l4, l5).isUnused() &&
                            SjisEntry(jpl1, jpl2, jpl3, jpl4, jpl5).isUnused()
                        ) {
                            out.writeNullTerminatedString(l1, charset = charset)
                            out.writeNullTerminatedString(l2, charset = charset)
                            out.writeNullTerminatedString(l3, charset = charset)
                            out.writeNullTerminatedString(l4, charset = charset)
                            out.writeNullTerminatedString(l5, charset = charset)
                        } else {
                            texts.forEach { text ->
                                val newText = translation.getTranslation(entryCount, translationOffset)
                                if (newText.isBlank()) {
                                    out.writeNullTerminatedString(text, charset)
                                } else {
                                    out.writeNullTerminatedString(newText, charset)
                                }
                                entryCount++
                            }
                        }
                    } else {
                        out.writeNullTerminatedString(l1, charset = charset)
                        out.writeNullTerminatedString(l2, charset = charset)
                        out.writeNullTerminatedString(l3, charset = charset)
                        out.writeNullTerminatedString(l4, charset = charset)
                        out.writeNullTerminatedString(l5, charset = charset)
                    }
                } catch (ignored: EOFException) {
                }
            }
            close()
        }
        out.align(16)
        out.close()
    }
}
