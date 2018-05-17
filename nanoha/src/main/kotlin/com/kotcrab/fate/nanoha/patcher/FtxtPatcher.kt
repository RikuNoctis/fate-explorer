package com.kotcrab.fate.nanoha.patcher

import com.google.common.base.CharMatcher
import com.google.common.io.BaseEncoding
import com.kotcrab.fate.io.LERandomAccessFile
import com.kotcrab.fate.nanoha.editor.core.TextEntry
import com.kotcrab.fate.util.getSubArrayPos
import com.kotcrab.fate.util.seek
import com.kotcrab.fate.util.toUnsignedInt
import java.io.File

/** @author Kotcrab */
class FtxtPatcher(private val srcAsmFile: File,
                  private val entries: List<TextEntry>) {
    fun patchTo(targetFile: File) {
        val ftxtSectOffset = getSubArrayPos(srcAsmFile.readBytes(), "FTXT".toByteArray())
        if (ftxtSectOffset == -1) error("This ASM file does not have FTXT section")

        srcAsmFile.copyTo(targetFile, overwrite = true)
        with(LERandomAccessFile(targetFile, "rw")) {
            seek(ftxtSectOffset + 0x8)
            val entriesCount = readInt()

            val newPointers = mutableListOf<Int>()
            seek(ftxtSectOffset + 0x28)
            val textSectOffset = readInt() + 0x10
            seek(filePointer - 0x4)
            writeInt(textSectOffset)
            seek(ftxtSectOffset + textSectOffset - 0x10)
            write(ByteArray(0x10, { 0xFF.toByte() })) //writing some extra padding for pointers section

            seek(ftxtSectOffset + textSectOffset)
            val textStart = filePointer
            repeat(entriesCount - 1) { entryIdx ->
                val newText = with(entries[entryIdx]) {
                    if (translation.isBlank())
                        "<missing translation id ${srcAsmFile.nameWithoutExtension.filter { it.isDigit() }}, $entryIdx>"
                    else
                        translation
                }
                newPointers.add(filePointer.toInt() - ftxtSectOffset - textSectOffset)

                val textIter = newText
                        .replace("\n", "")
                        .replace("@", "\\x0080")
                        .replace("W", "\\x0082")
                        .replace("―", "\\x00A0")
                        .replace("–", "\\x00A0")
                        .replace("－", "\\x00A0") //yep those map to the same
                        .replace("’", "\\x00A1")
                        .replace("“", "\\x00A2")
                        .replace("”", "\\x00A3")
                        .replace("…", "\\x00A4")
                        .replace("ä", "\\x0084")
                        .replace("ç", "\\x0085")
                        .replace("ü", "\\x0086")
                        .replace("‘", "\\x0088")
                        .replace("♪", "\\x00A5") //special
                        .replace("←", "\\x00A6")
                        .replace("「", "\\x00A7")
                        .replace("」", "\\x00A8")
                        .replace("☆", "\\x00A9")
                        .toCharArray().iterator()
                while (textIter.hasNext()) {
                    val char = textIter.nextChar()
                    when {
                        char == '\\' -> {
                            val escapeChar = textIter.nextChar()
                            when (escapeChar) {
                                'n' -> {
                                    writeByte(0x0A)
                                    writeByte(0xF0)
                                }
                                'x' -> {
                                    val encodedStr = "${textIter.nextChar().toUpperCase()}${textIter.nextChar().toUpperCase()}" +
                                            "${textIter.nextChar().toUpperCase()}${textIter.nextChar().toUpperCase()}"
                                    val bytes = BaseEncoding.base16().decode(encodedStr)
                                    writeByte(bytes[1].toUnsignedInt())
                                    writeByte(bytes[0].toUnsignedInt())
                                }
                                else -> error("Unknown backslash escape sequence")
                            }
                        }
                        CharMatcher.ASCII.matches(char) -> {
                            writeByte(char.toInt())
                            writeByte(0x00)
                        }
                        else -> println("WARN: Can't map non ascii character $char")
                    }
                }
                writeByte(0xFF)
                writeByte(0xFF)
            }
            val textSectionSize = (filePointer - textStart).toInt()

            seek(ftxtSectOffset + 0x1C)
            writeInt(textSectionSize)

            seek(ftxtSectOffset + 0x24)
            val pointerSectOffset = readInt()
            seek(ftxtSectOffset + pointerSectOffset)
            newPointers.forEach { ptr ->
                while (readInt() == -1) {
                }
                seek(filePointer - 4L)
                writeInt(ptr)
            }
            close()
        }
    }
}
