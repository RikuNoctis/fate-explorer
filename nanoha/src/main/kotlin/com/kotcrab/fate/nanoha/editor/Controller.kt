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

package com.kotcrab.fate.nanoha.editor

import com.kotcrab.fate.nanoha.editor.core.FontLoader
import com.kotcrab.fate.nanoha.editor.core.FtxtLoader
import com.kotcrab.fate.nanoha.editor.core.TextEntryModel
import com.kotcrab.fate.nanoha.editor.core.TranslationIO
import com.kotcrab.fate.nanoha.editor.view.LoadingScreen
import com.kotcrab.fate.nanoha.editor.view.TranslateScreen
import javafx.scene.control.ButtonType
import kio.util.child
import tornadofx.Controller
import tornadofx.ItemViewModel
import tornadofx.confirmation
import tornadofx.observable
import java.awt.image.BufferedImage
import java.io.File

/** @author Kotcrab */
class Controller : Controller() {
    private val mainScreen: TranslateScreen by inject()
    private val loadingScreen: LoadingScreen by inject()

    private lateinit var workingDir: File
    private val asmDir: File by lazy {
        workingDir.child("asm")
    }
    private val fontDir: File by lazy {
        workingDir.child("font")
    }

    private val ftxtLoader = FtxtLoader()
    private val fontLoader = FontLoader()
    private val translationIO = TranslationIO()

    val textEntry = TextEntryModel()
    val navigation = NavigationStateModel()
    val search = SearchStateModel()

    fun setWorkingDirectory(file: File): Boolean {
        this.workingDir = file
        return arrayOf(workingDir, fontDir, asmDir).all { it.exists() }
    }

    fun init() {
        runAsync {
            updateMessage("Loading texts...")
            ftxtLoader.load(asmDir)
            println("Loaded ${ftxtLoader.getCount()} entries")

            updateMessage("Loading fonts...")
            fontLoader.load(fontDir)
            println("Loaded ${fontLoader.getCount()} fonts")

            updateMessage("Loading translation...")
            translationIO.load(workingDir, asmDir, ftxtLoader)
            println("Loaded ${translationIO.getEntryCount()} translation entries")
        } ui {
            navigation.item = NavigationState("", false, true)
            search.item = SearchState()
            textEntry.item = translationIO.getEntry(0)
            loadingScreen.close()
            mainScreen.openWindow()
            mainScreen.currentStage!!.setOnCloseRequest {
                mainScreen.closeModals()
                confirmation(
                    "Save changes",
                    "Save changes before quiting?",
                    ButtonType.YES,
                    ButtonType.NO,
                    ButtonType.CANCEL
                ) { button ->
                    if (button == ButtonType.YES) {
                        saveTranslation()
                    }
                    if (button == ButtonType.CANCEL) {
                        it.consume()
                    }
                }
            }
        }

        textEntry.globalIndex.addListener { _, _, newValue ->
            navigation.hasPrevious.value = newValue != 0
            navigation.hasNext.value = newValue != translationIO.getEntryCount() - 1
        }
    }


    fun gotoPreviousEntry() {
        if (navigation.hasPrevious.value == false) return
        textEntry.item = translationIO.getEntry(textEntry.globalIndex.value - 1)
    }

    fun gotoNextEntry() {
        if (navigation.hasNext.value == false) return
        textEntry.item = translationIO.getEntry(textEntry.globalIndex.value + 1)
    }

    fun gotoEntry(idx: Int) {
        textEntry.item = translationIO.getEntry(idx)
    }

    fun getEntryCount(): Int {
        return translationIO.getEntryCount()
    }

    fun saveTranslation() {
        translationIO.saveTranslation()
    }

    fun getGlyphCount(chapter: Int): Int {
        return fontLoader.getGlyphCount(chapter)
    }

    fun getGlyph(chapter: Int, glyphId: Int): BufferedImage {
        return fontLoader.getGlyph(chapter, glyphId)
    }

    fun search() {
        search.commit {
            search.results.value.clear()
            search.results.value.addAll(translationIO.searchEntries(search.query.value))
            val res = search.results.value.size
            if (res == 1) {
                search.resultCount.value = "1 result"
            } else {
                search.resultCount.value = "${search.results.value.size} results"
            }
        }
    }
}

data class NavigationState(
    val goto: String = "",
    val hasPrevious: Boolean,
    val hasNext: Boolean
)

class NavigationStateModel : ItemViewModel<NavigationState>() {
    val goto = bind(NavigationState::goto)
    val hasPrevious = bind(NavigationState::hasPrevious)
    val hasNext = bind(NavigationState::hasNext)
}

data class SearchState(var query: String = "") {
    val results = mutableListOf<String>().observable()
    var resultCount = ""
}

class SearchStateModel : ItemViewModel<SearchState>() {
    val query = bind(SearchState::query)
    val results = bind(SearchState::results)
    val resultCount = bind(SearchState::resultCount)
}
