package com.kotcrab.fate.nanoha.editor.core

import com.google.gson.GsonBuilder
import com.kotcrab.fate.util.child
import com.kotcrab.fate.util.fromJson
import com.kotcrab.fate.util.relativizePath
import tornadofx.ItemViewModel
import java.io.File

/** @author Kotcrab */
class TranslationIO {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private lateinit var translationFile: File
    private lateinit var asmDir: File
    private lateinit var ftxtLoader: FtxtLoader

    private val entries = mutableListOf<TextEntry>()

    fun load(workingDir: File, asmDir: File, ftxtLoader: FtxtLoader) {
        translationFile = workingDir.child("translation.json")
        this.asmDir = asmDir
        this.ftxtLoader = ftxtLoader

        if (translationFile.exists()) {
            loadTranslation()
        } else {
            createInitialTranslationFile(asmDir, ftxtLoader)
        }
    }

    private fun loadTranslation() {
        entries.clear()
        entries.addAll(gson.fromJson<ArrayList<TextEntry>>(translationFile.bufferedReader()))
        linkEntries()
    }

    private fun linkEntries() {
        entries.forEach {
            it.encodedText = ftxtLoader.getEntry(asmDir.child(it.relPath), it.ftxtIndex)
        }
    }

    fun saveTranslation() {
        translationFile.bufferedWriter().use {
            gson.toJson(entries, it)
        }
    }

    private fun createInitialTranslationFile(asmDir: File, ftxtLoader: FtxtLoader) {
        var globalIndex = 0
        ftxtLoader.entries.forEach { file, codePoints ->
            val relPath = file.relativizePath(asmDir)
            codePoints.forEachIndexed { index, _ ->
                entries.add(TextEntry(globalIndex, relPath, index, "", ""))
                globalIndex++
            }
        }
        linkEntries()
        saveTranslation()
    }

    fun getEntry(idx: Int): TextEntry {
        return entries[idx]
    }

    fun getEntryCount(): Int {
        return entries.size
    }

    fun searchEntries(query: String): List<String> {
        println("Search for $query")
        val res = mutableListOf<String>()
        res.addAll(entries.filter { it.translation.contains(query, ignoreCase = true) }
                .map { "${it.globalIndex + 1} - ${it.translation.replace("\n", "")}" })
        res.addAll(entries.filter { it.notes.contains(query, ignoreCase = true) }
                .map { "${it.globalIndex + 1} - ${it.notes.replace("\n", "")}" })
        return res
    }
}

@Suppress("ArrayInDataClass")
data class TextEntry(val globalIndex: Int,
                     val relPath: String,
                     val ftxtIndex: Int,
                     var translation: String,
                     var notes: String,
                     @Transient var encodedText: IntArray? = null
)

class TextEntryModel : ItemViewModel<TextEntry>() {
    val globalIndex = bind(TextEntry::globalIndex)
    val relPath = bind(TextEntry::relPath)
    val ftxtIndex = bind(TextEntry::ftxtIndex)
    val translation = bind(TextEntry::translation)
    val notes = bind(TextEntry::notes)
    val encodedText = bind(TextEntry::encodedText)
}
