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

package com.kotcrab.fate.nanoha

import com.kotcrab.fate.projectBase
import com.kotcrab.fate.util.child

/** @author Kotcrab */
val gearsBase = projectBase.child("Gears")
val gearsTools = gearsBase.child("Tools")
val gearsPatch = gearsBase.child("_Patch")
val gearsUnpackBase = gearsBase.child("Unpack")
val gearsISOUnpack = gearsUnpackBase.child("ISO")
val gearsPACUnpack = gearsUnpackBase.child("PAC")
val gearsOutput = gearsBase.child("TMP")

val acesBase = projectBase.child("Aces")
val acesTools = acesBase.child("Tools")
val acesUnpackBase = acesBase.child("Unpack")
val acesISOUnpack = acesUnpackBase.child("ISO")
val acesPACUnpack = acesUnpackBase.child("PAC")
val acesOutput = acesBase.child("TMP")
