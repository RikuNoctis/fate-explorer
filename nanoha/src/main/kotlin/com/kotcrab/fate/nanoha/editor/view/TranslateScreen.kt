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

package com.kotcrab.fate.nanoha.editor.view

import com.kotcrab.fate.nanoha.editor.Controller
import com.kotcrab.fate.nanoha.editor.app.APP_TITLE
import com.kotcrab.fate.nanoha.editor.app.ReadOnlyStringConverter
import com.kotcrab.fate.nanoha.editor.app.Styles.Companion.appScreen
import com.kotcrab.fate.nanoha.editor.app.Styles.Companion.paddedLabel
import com.kotcrab.fate.util.toHex
import javafx.embed.swing.SwingFXUtils
import javafx.scene.layout.Priority
import javafx.stage.Modality
import tornadofx.*

class TranslateScreen : View(APP_TITLE) {
    val controller: Controller by inject()
    val textEntry = controller.textEntry
    val navigation = controller.navigation
    val searchModal = find<SearchScreen>()

    val controlCodes = StringBuilder()

    override val root = form {
        addClass(appScreen)
        setMinSize(680.0, 540.0)

        vbox {
            vgrow = Priority.ALWAYS

            add(TranslationView())

            button("Copy control codes to translation") {
                action {
                    textEntry.translation.value = textEntry.translation.value + controlCodes.toString()
                }
            }

            separator()
            add(NavigationView())
            separator()
            add(StatusView())
        }
    }

    fun closeModals() {
        searchModal.close()
    }

    inner class TranslationView : View() {
        override val root = borderpane {
            vgrow = Priority.ALWAYS

            center = vbox {
                style {
                    padding = box(0.px, 10.px, 0.px, 0.px)
                }
                label("Japanese")
                add(JapaneseView())
                label("Translation")
                textarea(textEntry.translation) {
                    whenDocked { requestFocus() }
                }
            }
            right = vbox {
                prefWidth = 250.0
                hgrow = Priority.ALWAYS
                label("Notes")
                textarea(textEntry.notes) {
                    vgrow = Priority.ALWAYS
                }
            }
        }
    }

    inner class JapaneseView : View() {
        override val root = flowpane {
            vgrow = Priority.ALWAYS
            style {
                backgroundColor = multi(c("000000"))
            }

            fun addElems() {
                clear()
                controlCodes.setLength(0)
                val chapter = textEntry.item.relPath.substring(0, 2).toInt()
                val glyphCount = controller.getGlyphCount(chapter)
                textEntry.encodedText.value.forEach { glyphId ->
                    if (glyphId > glyphCount) {
                        val stringRep = "\\x${glyphId.toHex()}"
                        controlCodes.append(stringRep)
                        label(stringRep) {
                            style {
                                textFill = c("#ff0000")
                            }
                        }
                    } else {
                        imageview(SwingFXUtils.toFXImage(controller.getGlyph(chapter, glyphId), null))
                    }
                }
            }

            addElems()
            textEntry.encodedText.addListener { _, _, _ ->
                addElems()
            }
        }
    }

    inner class NavigationView : View() {
        override val root = hbox {
            label("Goto") {
                addClass(paddedLabel)
            }
            textfield(navigation.goto) {
                validator {
                    val input = it!!
                    when {
                        input.isInt() == false -> error("Enter a number")
                        input.toInt() !in 1..controller.getEntryCount() -> error("Number out of range")
                        else -> null
                    }
                }
                action {
                    if (navigation.valid.value) {
                        textEntry.commit {
                            controller.gotoEntry(navigation.goto.value.toInt() - 1)
                        }
                    }
                }
            }
            button("Previous") {
                enableWhen(navigation.hasPrevious)
                action {
                    textEntry.commit {
                        controller.gotoPreviousEntry()
                    }
                }
            }
            button("Next") {
                enableWhen(navigation.hasNext)
                action {
                    textEntry.commit {
                        controller.gotoNextEntry()
                    }
                }
            }
            button("Search") {
                action {
                    searchModal.openModal(modality = Modality.NONE, owner = primaryStage)
                }
            }
            region {
                hgrow = Priority.ALWAYS
            }
            label("File: ${textEntry.relPath.value}") {
                addClass(paddedLabel)
                textEntry.relPath.addListener { _, _, newValue ->
                    text = "File: $newValue"
                }
            }
            label {
                addClass(paddedLabel)
                bind(textEntry.ftxtIndex, readonly = true, converter = ReadOnlyStringConverter({ "Index: ${it + 1}" }))
            }
        }
    }

    inner class StatusView : View() {
        override val root = hbox {
            hgrow = Priority.ALWAYS
            label {
                addClass(paddedLabel)
                bind(textEntry.globalIndex, readonly = true,
                        converter = ReadOnlyStringConverter({ "Entry ${it + 1} of ${controller.getEntryCount()}" }))
            }
            region {
                hgrow = Priority.ALWAYS
            }
            button("Save All") {
                action {
                    textEntry.commit {
                        controller.saveTranslation()
                    }
                }
            }
        }
    }

    override fun onDock() {
        textEntry.validate(decorateErrors = false)
    }
}
