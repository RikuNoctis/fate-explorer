package com.kotcrab.fate.nanoha.editor.view

import com.kotcrab.fate.nanoha.editor.Controller
import com.kotcrab.fate.nanoha.editor.app.APP_TITLE
import com.kotcrab.fate.nanoha.editor.app.Styles.Companion.appScreen
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*

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
