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

package com.kotcrab.fate.nanoha.cli

import com.kotcrab.fate.nanoha.acesOutput
import com.kotcrab.fate.util.ScriptEditorEntry
import com.kotcrab.fate.util.ScriptEditorFilesWriter
import kio.KioInputStream
import kio.util.WINDOWS_932
import kio.util.child
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */

fun main(args: Array<String>) {
    EbootTextDump(acesOutput.child("UTF8.bin"), Charsets.UTF_8, "eboot")
    EbootTextDump(acesOutput.child("SHIFT_JIS.bin"), Charsets.WINDOWS_932, "eboot2")
    println("Done")
}

class EbootTextDump(srcBin: File, charset: Charset, outDirName: String) {
    init {
        val entries = mutableListOf<ScriptEditorEntry>()
        with(KioInputStream(srcBin)) {
            while (!eof()) {
                skipNullBytes()
                if (eof()) break
                val jp = readNullTerminatedString(charset)
                entries.add(ScriptEditorEntry(jp))
            }
        }
        ScriptEditorFilesWriter(acesOutput.child(outDirName), entries)
    }
}
