package com.kotcrab.fate.nanoha.cli

import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.nanoha.acesOutput
import com.kotcrab.fate.util.ScriptEditorEntry
import com.kotcrab.fate.util.ScriptEditorFilesWriter
import com.kotcrab.fate.util.WINDOWS_932
import com.kotcrab.fate.util.child
import java.io.File
import java.nio.charset.Charset

fun main(args: Array<String>) {
    EbootTextDump(acesOutput.child("UTF8.bin"), Charsets.UTF_8, "eboot")
    EbootTextDump(acesOutput.child("SHIFT_JIS.bin"), Charsets.WINDOWS_932, "eboot2")
    println("Done")
}

class EbootTextDump(srcBin: File, charset: Charset, outDirName: String) {
    init {
        val entries = mutableListOf<ScriptEditorEntry>()
        with(FateInputStream(srcBin)) {
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
