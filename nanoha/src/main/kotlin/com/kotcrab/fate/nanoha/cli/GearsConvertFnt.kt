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

import com.kotcrab.fate.nanoha.file.FntFile
import com.kotcrab.fate.nanoha.gearsOutput
import com.kotcrab.fate.nanoha.gearsPACUnpack
import com.kotcrab.fate.util.child

/** @author Kotcrab */
fun main(args: Array<String>) {
    arrayOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12").forEach {
        println("Processing $it...")
        val font = gearsPACUnpack.child("story$it.fnt")
        FntFile(font).writeToPng(gearsOutput.child("story$it.png"))
    }
}
