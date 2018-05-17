package com.kotcrab.fate.nanoha.editor.app

import tornadofx.*

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
