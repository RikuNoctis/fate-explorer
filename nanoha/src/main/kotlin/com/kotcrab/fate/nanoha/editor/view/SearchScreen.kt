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
import com.kotcrab.fate.nanoha.editor.app.Styles.Companion.appScreen
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*

/** @author Kotcrab */
class SearchScreen : View("$APP_TITLE - Search") {
    private val controller: Controller by inject()
    private val search = controller.search

    override val root = form {
        addClass(appScreen)
        setMinSize(400.0, 120.0)
        vbox {
            alignment = Pos.CENTER_LEFT
            vgrow = Priority.ALWAYS
            hbox {
                label("Search")
                region {
                    hgrow = Priority.ALWAYS
                }
                label(search.resultCount)
            }

            textfield(search.query) {
                validator {
                    val input = it!!
                    when {
                        input.isEmpty() -> error("Enter a query")
                        else -> null
                    }
                }
                action {
                    if (search.valid.value) {
                        if (search.commit()) {
                            controller.search()
                        }
                    }
                }
            }

            listview(search.results) {
                onDoubleClick {
                    if (selectedItem != null) {
                        controller.gotoEntry(selectedItem!!.split(" -", limit = 2)[0].toInt() - 1)

                    }
                }
                vgrow = Priority.ALWAYS
            }
        }

    }

}
