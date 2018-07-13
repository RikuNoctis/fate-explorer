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

package com.kotcrab.fate.nanoha.patcher

import com.google.common.io.BaseEncoding
import com.kotcrab.fate.io.FateInputStream
import com.kotcrab.fate.nanoha.editor.core.TextEntry
import com.kotcrab.fate.nanoha.file.PacFile
import com.kotcrab.fate.nanoha.file.PacFileEntry
import com.kotcrab.fate.nanoha.file.Tim2File
import com.kotcrab.fate.nanoha.gearsPatch
import com.kotcrab.fate.util.*
import org.apache.commons.exec.PumpStreamHandler
import java.io.File
import java.io.RandomAccessFile

/** @author Kotcrab */
fun main(args: Array<String>) {
    GearsPatcher()
}

class GearsPatcher {
    private val baseDir = gearsPatch

    private val srcDir = baseDir.child("src")
    private val xdeltaDir = baseDir.child("xdelta")
    private val buildDir = baseDir.child("build")
    private val publicDestDir = baseDir.child("Gears of Destiny Patcher")
    private val isoSrcDir = baseDir.child("src/iso")
    private val asmSrcDir = baseDir.child("src/asm")
    private val customIsoFilesDir = baseDir.child("src/customIsoFiles")
    private val customPacFilesDir = baseDir.child("src/customPacFiles")
    private val customGeneratedPacFilesDir = baseDir.child("src/customGeneratedPacFiles")
    private val isoFileList = baseDir.child("src/iso_file_list.txt")
    private val isoLbaList = baseDir.child("src/iso_lba_list.txt")
    private val srcEboot = baseDir.child("src/ULJS00385.BIN")
    private val translationJson = baseDir.child("translation/translation.json")

    private val isoBuildDir = baseDir.child("build/iso")
    private val customIsoFilesJson = baseDir.child("build/customIsoFiles.json")
    private val outIso = baseDir.child("build/gears.iso")

    private val mkisofsTool = baseDir.child("tools/mkisofs.exe")
    private val xdeltaTool = baseDir.child("tools/xdelta.exe")

    init {
        println("--- Nanoha Gears of Destiny Internal Patcher ---")
        println("Working directory: ${baseDir.absolutePath}")
        if (baseDir.exists() == false) error("Working directory does not exist")
        patchNamesTextureAtlas()
        insertTranslation()
        patchEboot()
        buildCustomFonts()
        buildCustomPacs()
        copyCustomIsoFiles()
        buildIso()
        fixSoundLba()
        fixFileIndex()
        createXdeltaPatches()
        copyXdeltaPatches()
        println("Build completed")
    }

    private fun patchNamesTextureAtlas() {
        println("--- Patching names texture ---")
        val texWorks = srcDir.child("texWorks")
        val texName = "msg_name.tm2"
        val out = customGeneratedPacFilesDir.child(texName)
        Tm2NamesPatcher(texWorks.child(texName), texWorks.child("$texName.png")).patchTo(out)
        Tim2File(out).writeToPng(texWorks.child("convertBackTest.png"))

        val sprName = "msg_name.spr"
        val sprDir = texWorks.child("sprOut")
        val sprConvertBackDir = texWorks.child("sprOutConvertBack")
        sprDir.mkdir()
        sprConvertBackDir.mkdir()
        SprSplitter(texWorks.child(sprName), texWorks.child("msg_name_orig.tm2.png")).splitTo(sprDir)
        SprNamesPatcher(texWorks.child(sprName), texWorks.child("bounds.txt"), 256, 256).patchTo(customGeneratedPacFilesDir.child(sprName))
        println("Post check SPR")
        SprSplitter(customGeneratedPacFilesDir.child(sprName), texWorks.child("$texName.png")).splitTo(sprConvertBackDir)
    }

    private fun insertTranslation() {
        println("--- Inserting translation ---")
        val translation = translationJson.readJson<ArrayList<TextEntry>>()
        asmSrcDir.listFiles().forEach { srcAsm ->
            println("Patching FTXT ${srcAsm.name}...")
            val entries = translation.filter { it.relPath.endsWith(srcAsm.name) }
            FtxtPatcher(srcAsm, entries).patchTo(customGeneratedPacFilesDir.child(srcAsm.name))
        }
    }

    private fun patchEboot() {
        println("--- Patching EBOOT ---")
        val ebootPatches = EbootPatches()
        val extPatchBase = 0x090C2BA0
        val extPatchHex = ebootPatches.assembleExternalPatch(extPatchBase)
        with(RandomAccessFile(customPacFilesDir.child("loading.spr"), "rw")) {
            seek(0x4D0)
            write(BaseEncoding.base16().decode(extPatchHex))
            close()
        }

        val patchedEboot = srcEboot.copyTo(srcEboot.resolveSibling("${srcEboot.nameWithoutExtension}_patched.bin"),
                overwrite = true)
        EbootPatcher(srcEboot, patchedEboot, ebootPatches.assembleEbootPatches(extPatchBase), 0, 2)
        val ebootDir = isoBuildDir.child("PSP_GAME\\SYSDIR")
        patchedEboot.copyTo(ebootDir.child("BOOT.BIN"), overwrite = true)
        patchedEboot.copyTo(ebootDir.child("EBOOT.BIN"), overwrite = true)
    }

    private fun buildCustomFonts() {
        val fontDir = srcDir.child("font")
        println("--- Writing custom fonts ---")
        MergeHieroFont(fontDir.child("hiero"), fontDir.child("v2"))
        val newFont = fontDir.child("v2/story01_new.fnt")
        arrayOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12").forEach {
            newFont.copyTo(customGeneratedPacFilesDir.child("story$it.fnt"), overwrite = true)
        }
    }

    private fun buildCustomPacs() {
        println("--- Building custom PACs --- ")
        customIsoFilesDir.walk().toList()
                .filter { it.isFile }
                .filter { it.extension == "pac" }
                .forEach { it.delete() }

        val pacFiles: Map<String, List<String>> = srcDir.child("pac_files.json").readJson()
        val lzsMap: Map<String, String> = srcDir.child("lzs_map.json").readJson()

        val pacToBuildList = mutableMapOf<String, MutableList<File>>()
        val newPacFiles = customGeneratedPacFilesDir.listFiles().toMutableList()
        newPacFiles.addAll(customPacFilesDir.listFiles())
        newPacFiles.forEach { modifiedFile ->
            val pacTargets = pacFiles[modifiedFile.name]
            if (pacTargets == null) {
                println("WARN: Failed to find target PAK for file: ${modifiedFile.name}")
                return@forEach
            }
            pacTargets.forEach { relPakPath ->
                pacToBuildList.getOrPut(relPakPath, { mutableListOf() }).add(modifiedFile)
            }
        }

        val pacDir = isoSrcDir.child("PSP_GAME/USRDIR")
        pacToBuildList.forEach { pacName, newFiles ->
            val pacEntries = PacFile(pacDir.child(pacName)).entries.toMutableList()
            newFiles.forEach { newFile ->
                val entryIndex = pacEntries.indexOfFirst { it.fileName == newFile.name }
                val entry = pacEntries[entryIndex]
                val lzsName = lzsMap[newFile.name]
                val newBytes = if (lzsName == null) {
                    newFile.readBytes()
                } else {
                    LzsEncoder(newFile, lzsName).compressedBytes
                }
                pacEntries[entryIndex] = PacFileEntry(entry.fileName, newFile.length().toInt(), entry.timet, newBytes)
            }
            println("Writing modified PAC $pacName...")
            PacWriter(pacEntries).writeToFile(customIsoFilesDir.child("PSP_GAME/USRDIR/$pacName"))
        }
    }

    private fun copyCustomIsoFiles() {
        println("--- Copying custom ISO files ---")
        if (isoBuildDir.listFiles().isEmpty()) {
            println("Performing first time ISO copy, this might take a while...")
            isoSrcDir.copyRecursively(isoBuildDir)
        }
        val previouslyCopiedFiles: ArrayList<String> = if (customIsoFilesJson.exists()) {
            stdGson.fromJson(customIsoFilesJson.bufferedReader())
        } else {
            ArrayList()
        }
        val copiedFiles = mutableListOf<String>()
        walkDir(customIsoFilesDir, processFile = { file ->
            val relPath = file.relativizePath(customIsoFilesDir)
            println("Copying $relPath")
            copiedFiles.add(relPath)
            previouslyCopiedFiles.remove(relPath)
            file.copyTo(isoBuildDir.child(relPath), overwrite = true)
        })
        previouslyCopiedFiles.forEach { relPath ->
            println("Restoring unchanged, stock file $relPath")
            isoSrcDir.child(relPath).copyTo(isoBuildDir.child(relPath), overwrite = true)
        }
        customIsoFilesJson.bufferedWriter().use { stdGson.toJson(copiedFiles, it) }
    }

    fun buildIso() {
        println("--- Writing ISO ---")
        execute(mkisofsTool, workingDirectory = baseDir, streamHandler = PumpStreamHandler(System.out, System.out),
                args = arrayOf("-sort", isoFileList.toRelativeNixPath(), "-iso-level", "4", "-xa",
                        "-sysid", "\"PSP GAME\"", "-A", "\"PSP GAME\"", "-V", "NANOHA", "-publisher", "NANOHA",
                        "-o", outIso.toRelativeNixPath(), isoBuildDir.toRelativeNixPath()))
    }

    private fun fixSoundLba() {
        println("--- Fixing sound LBA ---")
        with(RandomAccessFile(outIso, "rw")) {
            val soundLbaMagic = BaseEncoding.base16()
                    .decode("73 6F 75 6E 64 5F 6C 62 61 5F 73 69 7A 65 2E 62 69 6E 00 00 00 00 00 0D 55 58 41".replace(" ", ""))
            seek(getSubArrayPos(soundLbaMagic) - 0x1B)
            val soundLbaFilePos = readInt() * 2048L
            fixLbaTable(this, soundLbaFilePos)
            seek(soundLbaFilePos)
            val newBytes = ByteArray(0xCB88)
            read(newBytes)
            buildDir.child("sound_lba_size_new.bin").writeBytes(newBytes)
            close()
        }
    }

    private fun fixLbaTable(isoRaf: RandomAccessFile, soundLbaFilePos: Long) {
        data class LbaEntry(val fileName: String, val extent: Int, var newExtent: Int = -1, var soundLbaSizeEntryPos: Int = -1)

        val pathPrefix = "\\PSP_GAME\\USRDIR\\sound\\"
        val soundFiles = isoLbaList.readLines()
                .map { it.replace(" ", "").split(",") }
                .filter { it[1].startsWith(pathPrefix) }
                .map { LbaEntry(it[1].removePrefix(pathPrefix), it[0].toInt()) }
                .associateBy({ it.extent }, { it })
        with(FateInputStream(isoSrcDir.child("PSP_GAME/USRDIR/sound_lba_size.bin"))) {
            setPos(0xC0)
            while (!eof()) {
                val pos = count()
                val extent = readInt()
                readInt()
                if (extent == 0) continue
                val entry = soundFiles[extent] ?: error("No matching entry for extent at ${pos.toHex()}")
                entry.soundLbaSizeEntryPos = pos
            }
            close()
        }
        if (soundFiles.values.any { it.soundLbaSizeEntryPos == -1 }) {
            error("Unassigned soundLbaSizeEntryPos in LbaEntry found")
        }

        with(FateInputStream(outIso, littleEndian = false)) {
            val startPos = 0x1AFD0
            val endPos = 0x6C000
            setPos(startPos)
            while (count() < endPos) {
                val pos = count()
                val len = readByte().toUnsignedInt()
                if (len == 0) {
                    skipNullBytes()
                    continue
                }
                val nextEntry = pos + len
                skip(0x5)
                val newExtent = readInt()
                skip(0x17)
                val fileName = readNullTerminatedString()
                soundFiles.values.firstOrNull { it.fileName == fileName }?.newExtent = newExtent
                setPos(nextEntry)
            }
        }
        if (soundFiles.values.any { it.newExtent == -1 }) {
            error("Unassigned newExtent in LbaEntry found")
        }

        println("Writing new extents...")
        soundFiles.values.forEach { lbaEntry ->
            isoRaf.seek(soundLbaFilePos + lbaEntry.soundLbaSizeEntryPos)
            isoRaf.writeInt(lbaEntry.newExtent.toLittleEndian())
        }
    }

    private fun fixFileIndex() {
        println("--- Fixing file index ---")
        with(RandomAccessFile(outIso, "rw")) {
            val fileIndexLbaMagic = BaseEncoding.base16()
                    .decode("66 69 6C 65 5F 69 6E 64 65 78 2E 62 69 6E 00 00 00 00 00 0D 55 58 41".replace(" ", ""))
            seek(getSubArrayPos(fileIndexLbaMagic) - 0x1B)
            val indexFilePos = readInt() * 2048L
            fixFileIndexTable(this, indexFilePos)
            seek(indexFilePos)
            val newBytes = ByteArray(0x5DC0)
            read(newBytes)
            buildDir.child("file_index_new.bin").writeBytes(newBytes)
            close()
        }
    }

    private fun fixFileIndexTable(iso: RandomAccessFile, indexFilePos: Long) {
        val extentLoc = mutableMapOf<String, Long>()
        with(iso) {
            seek(indexFilePos)
            with(FateInputStream(readBytes(0x5DC0))) {
                while (!eof()) {
                    val fileName = readStringAndTrim(0x20)
                    extentLoc[fileName] = indexFilePos + count()
                    skip(0x8)
                }
            }
        }

        println("Writing new extents...")
        with(iso) {
            val startPos = 0xE060
            val endPos = 0x6BE60
            seek(startPos)
            while (filePointer < endPos) {
                val pos = filePointer
                val len = readByte().toUnsignedInt()
                if (len == 0) {
                    skipNullBytes()
                    continue
                }
                val nextEntry = pos + len
                skipBytes(0x5)
                val newExtent = readInt()
                skipBytes(0x17)
                val fileName = readNullTerminatedString()
                extentLoc[fileName]?.run {
                    seek(this)
                    writeInt(newExtent.toLittleEndian())
                }
                seek(nextEntry)
            }
        }
    }

    private fun createXdeltaPatches() {
        println("--- Creating xdelta patches ---")

        val xdeltaCmds = StringBuilder()
        walkDir(isoBuildDir, processFile = { file ->
            val relPath = file.relativizePath(isoBuildDir)
            if (relPath.startsWith("PSP_GAME/USRDIR/sound")) return@walkDir
            val stockFile = isoSrcDir.child(relPath)
            if (file.readBytes().contentEquals(stockFile.readBytes())) return@walkDir

            println("Generating patch for $relPath")
            val winRelPath = relPath.replace("/", "\\")
            val xdeltaFile = xdeltaDir.child("${file.name}.xdelta")
            execute(xdeltaTool, arrayOf("-f", "-s", stockFile, file, xdeltaFile))
            xdeltaCmds.appendLine("""echo Patching ${file.name}...""")
            xdeltaCmds.appendLine("""copy "build\iso\$winRelPath" "build\tmp\${file.name}" """)
            xdeltaCmds.appendLine(""""tools\xdelta.exe" -f -d -s "build\tmp\${file.name}" "patch\${xdeltaFile.name}" "build\iso\$winRelPath"""")
            xdeltaCmds.appendLine("""if errorlevel 1 goto :ERR""")
        })
        arrayOf("sound_lba_size", "file_index").forEach {
            run {
                val file = buildDir.child("${it}_new.bin")
                val relPath = "PSP_GAME/USRDIR/$it.bin"
                val stockFile = isoSrcDir.child(relPath)
                val xdeltaFile = xdeltaDir.child("$it.xdelta")
                val winRelPath = relPath.replace("/", "\\")
                println("Generating patch for $relPath")
                execute(xdeltaTool, arrayOf("-f", "-s", stockFile, file, xdeltaFile))
                xdeltaCmds.appendLine("""echo Patching ${stockFile.name}...""")
                xdeltaCmds.appendLine("""copy "build\iso\$winRelPath" "build\tmp\${file.name}" """)
                xdeltaCmds.appendLine(""""tools\xdelta.exe" -f -d -s "build\tmp\${file.name}" "patch\${xdeltaFile.name}" "build\iso\$winRelPath"""")
                xdeltaCmds.appendLine("""if errorlevel 1 goto :ERR""")
            }
        }

        val batFileOut = StringBuilder()
        batFileOut.append("""@echo off
if not "%ISO_READY%"=="yep" goto :NOPE

REM Did you know this is auto generated?

$xdeltaCmds
goto :FIN

:NOPE
echo Don't double click me :(
pause
goto :FIN

:ERR
exit /b 1

:FIN""")
        xdeltaDir.child("apply.bat").writeText(batFileOut.toString().replace("\n", "\r\n"))
    }


    private fun copyXdeltaPatches() {
        println("--- Copying patches to public dest dir ---")
        val patchDir = publicDestDir.child("patch")
        val result = patchDir.deleteRecursively()
        if (result == false) println("WARN: Public dest delete patch directory failed")
        xdeltaDir.copyRecursively(patchDir, overwrite = true)
        isoFileList.copyTo(patchDir.child(isoFileList.name), overwrite = true)
    }

    private fun File.toRelativeNixPath(): Any {
        return relativizePath(baseDir).replace("\\", "/")
    }
}
