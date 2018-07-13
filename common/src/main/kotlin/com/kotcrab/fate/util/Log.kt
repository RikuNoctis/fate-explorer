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

package com.kotcrab.fate.util

/** @author Kotcrab */
open class Log {
    private var inProgressSection = false

    open fun startProgress() {
        if (inProgressSection) fatal("Already in progress section")
        inProgressSection = true
    }

    open fun endProgress() {
        if (inProgressSection == false) fatal("Not in a progress section")
        inProgressSection = false
    }

    open fun progress(step: Int, maxStep: Int, msg: String) {
        if (inProgressSection == false) fatal("Not in a progress section")
        println("${step + 1}/$maxStep $msg")
    }

    open fun info(msg: String) {
        println(msg)
    }

    open fun warn(msg: String) {
        println("WARN: $msg")
    }

    open fun fatal(msg: String): Nothing {
        error(msg)
    }
}
