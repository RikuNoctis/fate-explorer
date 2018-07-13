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

package com.kotcrab.fate.patcher.extra

import com.kotcrab.fate.patcher.Translation
import java.io.File

/** @author Kotcrab */
class ExtraTranslation(jpFile: File, enFile: File = jpFile.resolveSibling("script-translation.txt"),
                       stripNewLine: Boolean = false,
                       overrides: Map<Int, String> = mapOf())
    : Translation(jpFile, enFile, stripNewLine, overrides) {

    fun getTranslation(index: Int, offset: Int = 0): String {
        return getRawTranslation(index, offset, true)
                .replace("\r\n", "\n")
                .replace("’", "'")
                .replace("“", "\"")
                .replace("”", "\"")
    }
}
