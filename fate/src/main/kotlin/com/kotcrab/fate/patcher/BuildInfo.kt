package com.kotcrab.fate.patcher

import java.util.*

class BuildInfo {
    val modifiedPaks: MutableList<String> = Collections.synchronizedList(mutableListOf())
}
