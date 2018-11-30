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

package com.kotcrab.fate.nanoha.cli

import com.kotcrab.fate.nanoha.*
import com.kotcrab.fate.tex.GmoTextureConverter
import kio.util.child

/** @author Kotcrab */
fun main(args: Array<String>) {
    val convertAces = false

    if (convertAces) {
        val gameTools = acesTools
        val gimConv = gameTools.child("""GimConv\GimConv.exe""")
        GmoTextureConverter(gimConv, acesPACUnpack, { it.extension == "gmo" })
            .convertTo(acesOutput.child("texgmo"))
    } else {
        val gameTools = gearsTools
        val gimConv = gameTools.child("""GimConv\GimConv.exe""")
        GmoTextureConverter(gimConv, gearsPACUnpack, { it.extension == "gmo" })
            .convertTo(gearsOutput.child("texgmo"))
    }
}
