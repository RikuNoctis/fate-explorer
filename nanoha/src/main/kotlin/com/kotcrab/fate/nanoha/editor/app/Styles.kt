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

import tornadofx.*

/** @author Kotcrab */
class Styles : Stylesheet() {
    companion object {
        val hbox by csselement("HBox")
        val vbox by csselement("VBox")

        val appScreen by cssclass()
        val paddedLabel by cssclass()
    }

    init {
        hbox {
            spacing = 10.px
        }
        vbox {
            spacing = 7.px
        }
        appScreen {
            padding = box(10.px)
            vgap = 7.px
            hgap = 10.px
        }
        paddedLabel {
            padding = box(4.px, 0.px, 0.px, 0.px)
        }
    }
}
