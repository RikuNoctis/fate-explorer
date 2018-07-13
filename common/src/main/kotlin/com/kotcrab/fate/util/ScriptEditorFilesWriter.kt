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

package com.kotcrab.fate.util

import java.io.File

/** @author Kotcrab */
class ScriptEditorFilesWriter(val outDir: File, val entries: List<ScriptEditorEntry>) {
    fun writeTo() {
        val jpOut = StringBuilder()
        val enOut = StringBuilder()
        val notesOut = StringBuilder()
        entries.forEach { entry ->
            jpOut.appendLine("${entry.jp}{end}\n")
            enOut.appendLine("${entry.en}{end}\n")
            notesOut.appendLine("${entry.note}{end}\n")
        }
        outDir.child("script-japanese.txt").writeText(jpOut.toString())
        outDir.child("script-translation.txt").writeText(enOut.toString())
        outDir.child("script-notes.txt").writeText(notesOut.toString())
    }
}

class ScriptEditorEntry(val jp: String, val en: String = "", val note: String = "")
