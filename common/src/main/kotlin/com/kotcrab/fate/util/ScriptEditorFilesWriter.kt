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
