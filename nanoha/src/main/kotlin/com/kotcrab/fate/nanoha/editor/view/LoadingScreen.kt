package com.kotcrab.fate.nanoha.editor.view

import com.kotcrab.fate.nanoha.editor.Controller
import com.kotcrab.fate.nanoha.editor.app.APP_TITLE
import com.kotcrab.fate.nanoha.editor.app.Styles.Companion.appScreen
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import tornadofx.*

class LoadingScreen : View(APP_TITLE) {
    private val controller: Controller by inject()
    private val status: TaskStatus by inject()

    override val root = form {
        addClass(appScreen)
        setMinSize(400.0, 120.0)
        vbox {
            alignment = Pos.CENTER
            vgrow = Priority.ALWAYS
            label(status.message)
        }
    }
}
