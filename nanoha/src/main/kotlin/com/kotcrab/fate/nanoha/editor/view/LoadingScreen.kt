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
