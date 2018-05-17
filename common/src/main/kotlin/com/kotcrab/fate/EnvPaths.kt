package com.kotcrab.fate

import java.io.File

/** @author Kotcrab */
private const val sysVariableName = "FE_PROJECT_BASE"
val projectBase by lazy {
    val path = System.getenv(sysVariableName) ?: error("$sysVariableName environment variable not defined")
    File(path)
}
