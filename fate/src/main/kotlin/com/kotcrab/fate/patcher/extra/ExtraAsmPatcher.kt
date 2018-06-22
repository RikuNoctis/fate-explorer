package com.kotcrab.fate.patcher.extra

import com.kotcrab.fate.file.PakFile
import com.kotcrab.fate.file.replaceEntry
import com.kotcrab.fate.file.sizeInGame
import com.kotcrab.fate.util.*
import kmips.Label
import kmips.Reg.*
import kmips.assembleAsHexString
import java.io.File
import javax.xml.bind.DatatypeConverter

class ExtraAsmPatcher {
    val bootPatches = mutableListOf<EbootPatch>()
    lateinit var extraPatchBytes: ByteArray

    var vars = Var()
    var funcs = Func()

    fun assemble(preloadPak: PakFile, workingDir: File) {
        val baseLoadAddress = 0x08C61CE0
        val pakEntries = preloadPak.entries

        //first phase text remapping is only needed for files that replaces PRELOAD.PAK files
        println("First patcher run...")

        pakEntries.forEach {
            val rep = workingDir.child(it.path)
            if (rep.exists()) {
                println("Patched file replaces PRELOAD.PAK entry ${it.path}")
                pakEntries.replaceEntry(it.path, rep.readBytes())
            }
        }

        val debugDumpPreloadContent = false
        if (debugDumpPreloadContent) {
            var fileAddress = baseLoadAddress
            pakEntries.forEach {
                println("${it.path} ${fileAddress.toHex()}")
                fileAddress += 0x40 + it.bytes.size
            }
            println("Next file at ${fileAddress.toHex()}")
        }

        val preloadInjectStart = baseLoadAddress + pakEntries.sizeInGame()
        println("Calculated that patch will be placed at ${preloadInjectStart.toHex()}")
        println("Second patcher run...")
        val patchStart = preloadInjectStart + 0x40
        println("Code will start at ${patchStart.toHex()}")
        val extraPatch = createPreloadCodePatch(patchStart)

        val patchBytes = mutableListOf<Byte>()
        patchBytes.addAll(DatatypeConverter.parseHexBinary(extraPatch).toList())
        extraPatchBytes = patchBytes.toByteArray()

        createBootPatches()
    }

    private fun createPreloadCodePatch(patchStart: Int) = assembleAsHexString(patchStart) {
        run {
            vars.address = virtualPc

            fun align16() {
                while (virtualPc % 16 != 0) data(0)
            }

            nop()
        }

        run {
            funcs.nameEntryCursorMovingDown = virtualPc
            val ctx = preserve(arrayOf(t0))
            val resumeStdCursorChange = Label()

            lw(t1, 0x310, s1) // load X pos
            bne(t1, zero, resumeStdCursorChange) // X cursor is on keyboard do nothing
            nop()

            li(t1, 1)
            beq(a0, t1, resumeStdCursorChange)
            li(a0, 0x3)

            addiu(a0, s6, 1) // cursor doesn't need change, load value that would be used normally

            label(resumeStdCursorChange)
            sw(a0, 0x314, s1)
            slti(a0, a0, 0x5)
            ctx.restoreAndExit()
        }

        run {
            funcs.nameEntryCursorMovingUp = virtualPc
            val defBranch = Label()
            val resumeStdCursorChange = Label()
            val changeCursor = Label()

            val ctx = preserve(arrayOf(t1))

            lw(t1, 0x310, s1) // load X pos
            bne(t1, zero, resumeStdCursorChange) // X cursor is on keyboard do nothing
            nop()

            li(t1, 2)
            beq(a0, t1, changeCursor)
            nop()

            b(resumeStdCursorChange)
            nop()

            label(changeCursor)
            li(a0, 0)
            b(resumeStdCursorChange)
            nop()

            label(resumeStdCursorChange)
            ctx.restore()

            bgez(a0, defBranch)
            sw(a0, 0x314, s1)

            //defNotBranch
            j(0x0887E86C)
            nop()
            label(defBranch)
            j(0x0887E87C)
            nop()
        }

        run {
            funcs.nameEntryCursorMovingLeft = virtualPc
            val defBranch = Label()
            val resumeStdCursorChange = Label()
            val fixCursorDown = Label()

            val ctx = preserve(arrayOf(t1, t2))

            bne(a0, zero, resumeStdCursorChange) // not switching from keyboard to tab, nothing to do
            nop()

            lw(t1, 0x314, s1)
            li(t2, 0x1)
            beq(t1, t2, fixCursorDown)
            nop()
            li(t2, 0x2)
            beq(t1, t2, fixCursorDown)
            nop()

            b(resumeStdCursorChange)
            nop()

            label(fixCursorDown)
            li(t2, 0x3)
            sw(t2, 0x314, s1)

            label(resumeStdCursorChange)
            ctx.restore()

            bgez(a0, defBranch)
            sw(a0, 0x310, s1)

            //defNotBranch
            j(0x0887E62C)
            nop()
            label(defBranch)
            j(0x0887E63C)
            nop()
        }

        run {
            funcs.nameEntryCursorMovingRight = virtualPc
            val resumeStdCursorChange = Label()
            val fixCursorDown = Label()
            val ctx = preserve(arrayOf(t1, t2))

            li(t1, 0x15)
            bne(t1, a0, resumeStdCursorChange) // no adjustment needed
            nop()

            lw(t1, 0x314, s1)
            li(t2, 0x1)
            beq(t1, t2, fixCursorDown)
            nop()
            li(t2, 0x2)
            beq(t1, t2, fixCursorDown)
            nop()

            b(resumeStdCursorChange)
            nop()

            label(fixCursorDown)
            li(t2, 0x3)
            sw(t2, 0x314, s1)

            label(resumeStdCursorChange)
            sw(a0, 0x310, s1)
            slti(a0, a0, 0x15)
            ctx.restoreAndExit()
        }

        run {
            funcs.statusScreenNameSpacing = virtualPc
            val ctx = preserve(arrayOf(t7))
            lui(t7, 0x4111)
            data(0x448F6800) //mtc1	t7,f13
            ctx.restoreAndExit()
        }

        nop()
    }

    private fun createBootPatches() {
        with(bootPatches) {
            val neMaxCharCount = 11
            patch("NameEntryCursorMovingDownFix") {
                change(0x0007A7A8) {
                    jal(funcs.nameEntryCursorMovingDown)
                    nop()
                }
            }
            patch("NameEntryCursorMovingUpFix") {
                change(0x0007A864) {
                    jal(funcs.nameEntryCursorMovingUp)
                    nop()
                }
            }
            patch("NameEntryCursorMovingRight") {
                change(0x0007A5F8) {
                    jal(funcs.nameEntryCursorMovingRight)
                    nop()
                }
            }
            patch("NameEntryCursorMovingLeft") {
                change(0x0007A624) {
                    jal(funcs.nameEntryCursorMovingLeft)
                    nop()
                }
            }
            patch("NameEntryCursor") {
                //fix last name spacing for entry screen
                change(0x00077748) {
                    lui(a1, 0x424C)
                }
                //fix cursor spacing
                change(0x00079A34) {
                    addiu(a0, a0, 0x61)
                }
                //increase cursor limit
                change(0x00079A24) { slti(a1, a0, neMaxCharCount) }
                change(0x00079A2C) { li(a0, neMaxCharCount - 1) }

                //fix manual cursor move
                //first name
                change(0x00078728) { li(a1, neMaxCharCount) }
                change(0x00078738) { slti(a0, a0, neMaxCharCount + 1) }
                //last name
                change(0x00078818) { slti(a0, a0, neMaxCharCount) }
            }
            patch("NameEntryInput") {
                //first name
                change(0x00077F60) { li(a1, neMaxCharCount) }
                change(0x00077F6C) { slti(a0, a0, neMaxCharCount + 1) }
                //last name
                change(0x00078080) { slti(t2, a1, neMaxCharCount + 1) }
                //indefinitely replace last character of name input
                change(0x0007810C) { sb(s1, 0x3F, a0) }
            }
            patch("NameEntryEmptyCheck") {
                //first name
                change(0x000775E4) { li(a2, neMaxCharCount) }
                //last name
                change(0x00077610) { li(a2, neMaxCharCount) }
            }
            patch("NameEntryDeletion") {
                change(0x00078564) {
                    val stdExit = Label()
                    stdExit.address = 0x0887C5AC

                    li(a1, neMaxCharCount - 1)
                    bne(a0, a1, stdExit)
                    nop()
                    li(s7, 0x20)
                    sb(s7, 0xE, sp)
                    sb(zero, 0xF, sp)
                    j(0x0887C5A4)
                    nop()
                }

                removeRelocation(0x00078578)
                removeRelocation(0x00078570)
            }
            patch("NameEntryInit") {
                change(0x00078F3C) { li(a2, 1) }
                change(0x00078F4C) { li(a2, 1) }
                change(0x00078F54) { addiu(s1, s1, 1) }
                change(0x00078F60) { addiu(s2, s2, 1) }
                change(0x00078F58) { slti(a0, s0, neMaxCharCount) }
            }
            patch("NameEntryConfirmPromptSpacingFix") {
                change(0x0007793C) { lui(a0, 0x4170) }
            }
            patch("StatusScreenNameSpacing") {
                change(0x0008C3C4) {
                    jal(funcs.statusScreenNameSpacing)
                }
                removeRelocation(0x0008C3C4)
            }
            patch("DefaultLineWidth") {
                change(0x00090AD4) { li(t1, 0x33) }
            }
            patch("InfoMatrixTooMuchOneCharacter") {
                //fix memset cleared area size, otherwise one junk character is present there
                change(0x00069148) { li(a2, 0x19) }
            }
        }
    }

    class Var {
        var address: Int = 0
    }

    class Func {
        var nameEntryCursorMovingDown = 0
        var nameEntryCursorMovingUp = 0
        var nameEntryCursorMovingLeft = 0
        var nameEntryCursorMovingRight = 0
        var statusScreenNameSpacing = 0
    }
}
