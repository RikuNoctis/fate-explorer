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

package com.kotcrab.fate.nanoha.editor.app

import com.kotcrab.fate.nanoha.editor.Controller
import com.kotcrab.fate.nanoha.editor.view.LoadingScreen
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.StringConverter
import tornadofx.App
import tornadofx.FX
import tornadofx.launch
import java.io.File

const val APP_TITLE = "Nanoha Script Editor"

/** @author Kotcrab */
class TranscribeApp : App(LoadingScreen::class, Styles::class) {
    private val controller: Controller by inject()

    override fun start(stage: Stage) {
        FX.layoutDebuggerShortcut = KeyCodeCombination(KeyCode.F11)

        if (parameters.raw.size == 0) {
            fatalError("Missing working directory", "Please specify working directory as first argument")
            return
        }
        val workingDir = parameters.raw[0].replace("\"", "")
        val valid = controller.setWorkingDirectory(File(workingDir))
        if (valid == false) {
            fatalError("Invalid working directory state", "At least one required file is missing")
            return
        }
        super.start(stage)
        controller.init()
    }
}

fun fatalError(header: String, text: String) {
    val alert = Alert(Alert.AlertType.ERROR)
    alert.initStyle(StageStyle.UTILITY)
    alert.title = APP_TITLE
    alert.headerText = header
    alert.contentText = text
    alert.showAndWait()
    Platform.exit()
}

object App {
    @JvmStatic
    fun main(args: Array<String>) {
        launch<TranscribeApp>(args)
    }
}

class ReadOnlyStringConverter<T>(private val convert: (T) -> String) : StringConverter<T>() {
    override fun toString(value: T): String {
        return convert(value)
    }

    override fun fromString(string: String?): T {
        throw UnsupportedOperationException("Not supported from read only converter")
    }
}
