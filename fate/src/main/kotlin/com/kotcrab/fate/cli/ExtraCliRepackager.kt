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

package com.kotcrab.fate.cli

import com.kotcrab.fate.fateBase
import com.kotcrab.fate.patcher.extra.ExtraRepackager
import kio.util.child
import java.io.File

fun main(args: Array<String>) {
    val toolkit = if (args.isEmpty()) fateBase.child("Extra Toolkit") else File(args.first())
    ExtraRepackager(
            toolkit,
            skipTranslationInsertion = false,
            skipCpkCreation = false
    ).buildAll()
}
