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
