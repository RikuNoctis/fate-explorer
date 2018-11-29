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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kotcrab.fate.file.CmpFile
import com.kotcrab.fate.file.PakFile
import com.kotcrab.fate.file.PakFileEntry
import com.kotcrab.fate.patcher.BuildInfo
import com.kotcrab.fate.patcher.CmpEncoder
import com.kotcrab.fate.patcher.PakReferenceMap
import com.kotcrab.fate.patcher.PakWriter
import com.kotcrab.fate.patcher.extra.file.*
import com.kotcrab.fate.util.EbootPatch
import com.kotcrab.fate.util.EbootPatcher
import kio.util.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.charset.Charset

/** @author Kotcrab */
class ExtraRepackager(val projectDir: File,
                      val skipTranslationInsertion: Boolean = false,
                      val skipCpkCreation: Boolean = false) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val cpkSrcDir = projectDir.child("src/cpk")
    private val customPakFilesDir = projectDir.child("src/customPakFiles")
    private val customCpkFilesDir = projectDir.child("src/customCpkFiles")
    private val isoSrcDir = projectDir.child("src/iso")
    private val ebootSrc = projectDir.child("src/ULUS10576.BIN")
    private val refsFile = projectDir.child("src/files.json")
    private val cpkOut = projectDir.child("build/cpk")
    private val isoDirOut = projectDir.child("build/iso")
    private val newPakSrcDir = projectDir.child("build/pak")
    private val buildInfoFile = projectDir.child("build/buildInfo.json")
    private val translationDir = projectDir.child("translation")
    private val cpkMakecTool = projectDir.child("tools/cpkmakec.exe")

    private lateinit var extraPatcher: ExtraAsmPatcher
    private val warnings = mutableListOf<String>()
    private val pakRefs: PakReferenceMap by lazy {
        if (refsFile.exists()) {
            gson.fromJson(refsFile.readText(), PakReferenceMap::class.java)
        } else {
            error("files.json is missing")
        }
    }

    fun buildAll(bootPatches: List<EbootPatch>? = null) {
        println("Project directory: ${projectDir.absolutePath}")
        if (projectDir.exists() == false) error("Project dir does not exist")
        checkTools()
        prepareCpkOut()
        cleanUpBuildDir()
        processTranslationUnits()
        copyCustomPakFiles()
        copyCustomCpkFiles()
        cleanUpBuildPakDir()
        assemblePatches()
        buildCustomPaks()
        buildCPK()
        patchEboot(bootPatches)
        println("'build/iso' directory is now bootable in PPSSPP, to create ISO run 'Make ISO.bat'")
        println("Build finished")

        if (warnings.size > 0) {
            println()
            println("${warnings.size} ${if (warnings.size == 1) "warning" else "warnings"}")
            warnings.forEach {
                println("WARN: $it")
            }
        } else {
            println("No warnings!")
        }
    }

    private fun cleanUpBuildPakDir() {
        walkDir(newPakSrcDir, processFile = { file ->
            val relPath = file.relativizePath(newPakSrcDir)
            if (relPath.startsWith("field/")) return@walkDir //handled manually in ExtraDatFilePatcher
            val relPakPath = pakRefs.files[relPath]!!.first()
            val pakFile = cpkSrcDir.child(relPakPath)
            val pak = when (pakFile.extension) {
                "pak" -> PakFile(pakFile)
                "cmp" -> PakFile(CmpFile(pakFile).getData())
                else -> error("Unknown PAK extension")
            }
            if (pak.getEntry(relPath).bytes.contentEquals(file.readBytes())) {
                println("Removing unchanged file $relPath")
                file.delete()
                if (file.parentFile.listFiles().isEmpty()) {
                    file.parentFile.delete()
                }
            }
        })
    }

    private fun checkTools() {
        if (cpkMakecTool.exists() == false) error("cpkmakec.exe is missing")
    }

    private fun cleanUpBuildDir() {
        println("Cleaning up build directory...")
        if (newPakSrcDir.deleteRecursively() == false) error("Failed to delete 'build/pak'")
        newPakSrcDir.mkdir()
        if (isoDirOut.deleteRecursively() == false) error("Failed to delete 'build/iso'")
        isoDirOut.mkdir()
    }

    private fun processTranslationUnits() {
        if (translationDir.exists() == false) error("Translation directory does not exist")
        if (skipTranslationInsertion) {
            warn("Translation insertion was skipped")
        }
        translationDir.listFiles().filter { it.isDirectory }.forEach {
            println("Processing translation unit: ${it.name}")
            when (it.name) {
                "dat" -> DatTranslationProcessor(it)
                "misc" -> MiscTranslationProcessor(it)
            }
        }
    }

    private fun copyCustomPakFiles() {
        println("Copying custom PAK files...")
        walkDir(customPakFilesDir, processFile = {
            if (it.isDirectory) return@walkDir
            val relPath = it.relativizePath(customPakFilesDir)
            if (relPath.startsWith(".git")) return@walkDir
            if (it.name == ".gitignore") return@walkDir
            val targetFile = newPakSrcDir.child(relPath)
            if (targetFile.exists()) warn("Custom pak file '$relPath' overwrites already existing file (most likely from translation unit)")
            it.copyTo(targetFile, overwrite = true)
        })
    }

    private fun copyCustomCpkFiles() {
        println("Copying custom CPK files...")
        walkDir(customCpkFilesDir, processFile = {
            if (it.isDirectory) return@walkDir
            val relPath = it.relativizePath(customCpkFilesDir)
            if (relPath.startsWith(".git")) return@walkDir
            if (it.name == ".gitignore") return@walkDir
            val targetFile = cpkOut.child(relPath)
            if (targetFile.exists()) warn("Custom CPK file '$relPath' overwrites already existing file")
            it.copyTo(targetFile, overwrite = true)
        })
    }

    private fun prepareCpkOut() {
        if (cpkOut.list().isEmpty()) {
            println("Copying CPK contents to build directory...")
            cpkSrcDir.copyRecursively(cpkOut)
        }
    }

    private fun assemblePatches() {
        extraPatcher = ExtraAsmPatcher()
        extraPatcher.assemble(PakFile(cpkSrcDir.child("pack/PRELOAD.PAK")), newPakSrcDir)
    }

    private fun buildCustomPaks() {
        println("Building modified PAK files...")
        val lastBuild = if (buildInfoFile.exists()) {
            gson.fromJson(buildInfoFile.readText(), BuildInfo::class.java)
        } else {
            BuildInfo()
        }
        val currentBuild = BuildInfo()

        val files = newPakSrcDir.walk().toList()
        val pakToBuildList = mutableMapOf<String, MutableList<String>>()
        files.forEach { modifiedFile ->
            if (modifiedFile.path.contains(".git")) return@forEach
            if (modifiedFile.isDirectory) return@forEach
            val relFilePath = modifiedFile.relativizePath(newPakSrcDir)
            val pakTarget = pakRefs.files[relFilePath] ?: error("Failed to find target PAK for file: $relFilePath")
            pakTarget.forEach { relPakPath ->
                val list = pakToBuildList.getOrPut(relPakPath) { mutableListOf() }
                list.add(relFilePath)
            }
        }

        runBlocking {
            println("Using 3 threads for PakBuildPool")
            val ctx = newFixedThreadPoolContext(3, "PakBuildPool")
            val jobs = mutableListOf<Job>()

            pakToBuildList.putIfAbsent("pack/PRELOAD.pak", mutableListOf())

            pakToBuildList.forEach { pakToBuild ->
                val job = launch(ctx) {
                    val sourcePakFile = cpkSrcDir.child(pakToBuild.key)
                    val sourcePakBytes = if (sourcePakFile.extension == "cmp") {
                        CmpFile(sourcePakFile).getData()
                    } else {
                        sourcePakFile.readBytes()
                    }
                    val sourcePak = PakFile(sourcePakBytes)
                    val newEntries = sourcePak.entries.toMutableList()
                    pakToBuild.value.forEach { path ->
                        var entryIndex = -1
                        newEntries.forEachIndexed { index, entry ->
                            if (entry.path == path) {
                                entryIndex = index
                            }
                        }
                        if (entryIndex == -1) {
                            error("Failed to replace entry $path in PAK $sourcePak")
                        }

                        val newFile = newPakSrcDir.child(path)
                        val oldEntry = newEntries[entryIndex]
                        newEntries[entryIndex] = PakFileEntry(oldEntry.index, oldEntry.path, padArray(newFile.readBytes()))
                    }
                    if (sourcePakFile.name == "PRELOAD.pak") {
                        newEntries.add(PakFileEntry(newEntries.last().index + 1, "hax/extrapatch.o", padArray(extraPatcher.extraPatchBytes)))
                    }
                    println("Writing modified PAK '${pakToBuild.key}'")
                    lastBuild.modifiedPaks.remove(pakToBuild.key)
                    currentBuild.modifiedPaks.add(pakToBuild.key)
                    val outFile = cpkOut.child(pakToBuild.key)
                    PakWriter(newEntries, outFile)
                    if (outFile.extension == "cmp") {
                        println("Compressing PAK to CMP '${pakToBuild.key}'")
                        outFile.writeBytes(CmpEncoder(outFile, false).getData())
                        if (outFile.length() > 2 * 1000 * 1000) {
                            PakWriter(newEntries, outFile)
                            outFile.writeBytes(CmpEncoder(outFile, true).getData())
                            warn("Second CMP pass for '${pakToBuild.key}'. New size: ${outFile.length()}")
                        }
                    }
                }
                jobs.add(job)
            }

            jobs.forEach { it.join() }
        }


        buildInfoFile.writeText(gson.toJson(currentBuild))
        lastBuild.modifiedPaks.forEach {
            println("'$it' was not modified during this build, restoring stock")
            cpkSrcDir.child(it).copyTo(cpkOut.child(it), overwrite = true)
        }
    }

    private fun buildCPK() {
        isoSrcDir.copyRecursively(isoDirOut)
        val outputCpk = isoDirOut.child("PSP_GAME/USRDIR/data.cpk")
        if (skipCpkCreation) {
            warn("Building CPK was skipped")
            return
        }
        println("Native CPK packager running...")
        Runtime.getRuntime().exec("""cmd /c "START /WAIT "" "${cpkMakecTool.absolutePath}" "${cpkOut.absolutePath}" "${outputCpk.absoluteFile}"
-align=2048 -code=UTF-8 -mode=FILENAME -mask"""").waitFor()
        File("cpkmaker.out.csv").delete()
    }

    private fun patchEboot(bootPatches: List<EbootPatch>?) {
        // look what patches are disabled and copy that
        if (bootPatches != null) {
            extraPatcher.bootPatches.forEach { it.active = true }
            bootPatches.filterNot { it.active }.forEach { customPatch ->
                extraPatcher.bootPatches.first { it.name == customPatch.name }.active = false
            }
        }

        println("Patching EBOOT.BIN...")
        val targetEbootBin = isoDirOut.child("PSP_GAME/SYSDIR/EBOOT.BIN")
        EbootPatcher(ebootSrc, targetEbootBin, extraPatcher.bootPatches, 0, 2)
        targetEbootBin.copyTo(targetEbootBin.resolveSibling("BOOT.BIN"), overwrite = true)
    }

    private fun warn(msg: String) {
        println("WARN: $msg")
        warnings.add(msg)
    }

    inner class DatTranslationProcessor(unitDir: File) {
        val translation = ExtraTranslation(unitDir.child("script-japanese.txt"))
        val translationOrig = ExtraTranslation(unitDir.child("script-japanese.txt"),
                unitDir.child("script-translation-orig.txt"))

        val skip = false

        init {
            checkDuplicates()
            if (skip == false) {
                val entriesMain: List<CombinedDatEntry> = Gson().fromJson(unitDir.child("entriesMain.json").readText())
                val entriesMisc: List<CombinedDatEntry> = Gson().fromJson(unitDir.child("entriesMisc.json").readText())
                val entries = mutableListOf<CombinedDatEntry>()
                entries.addAll(entriesMain)
                entries.addAll(entriesMisc)
                entries.groupBy { it.relPath }.forEach { relPath, fileEntries ->
                    patchFile(relPath, fileEntries)
                }
            } else {
                warn("DAT insertion was skipped, hope you got them cached")
            }
        }

        private fun checkDuplicates() {
            val translationOrig = ExtraTranslation(projectDir.child("translation/dat/script-japanese.txt"),
                    projectDir.child("translation/dat/script-translation-orig.txt"))
            val translation = ExtraTranslation(projectDir.child("translation/dat/script-japanese.txt"))
            translationOrig.enTexts
                    .mapIndexed { idx, text -> Pair(idx, text) }
                    .groupBy { it.second }
                    .filter { it.value.size > 1 }
                    .forEach { origEntry ->
                        val translatedSet = HashSet<Pair<String, String>>()
                        origEntry.value.forEach { set ->
                            val newText = translation.enTexts[set.first]
                            translatedSet.add(Pair(set.second, newText))
                        }
                        if (translatedSet.size > 1) {
                            warn("DAT English duplicate: ${translatedSet.first().first}")
                        }
                    }
        }

        private fun patchFile(path: String, fileEntries: List<CombinedDatEntry>) {
            println("Patching $path...")
            val relPakPath = pakRefs.files[path]!!.first()
            val pakFile = cpkSrcDir.child(relPakPath)
            val pak = when (pakFile.extension) {
                "pak" -> PakFile(pakFile)
                "cmp" -> PakFile(CmpFile(pakFile).getData())
                else -> error("Unknown PAK extension")
            }
            val newOut = newPakSrcDir.child(path)
            newOut.parentFile.mkdirs()
            ExtraDatFilePatcher(pak.getEntry(path).bytes, newOut, translation, translationOrig, fileEntries)
        }
    }

    inner class MiscTranslationProcessor(unitDir: File) {
        val translation = ExtraTranslation(unitDir.child("script-japanese.txt"))
        val translationOrig = ExtraTranslation(unitDir.child("script-japanese.txt"),
                unitDir.child("script-translation-orig.txt"))
        val entries: List<TranslationEntry> = Gson().fromJson(unitDir.child("entries.json").readText())

        init {
            patchSjisFile("paramData/chaDataTbl.sjis", 0)
            patchItemParam01File("cmn/item_param_01.bin", 920)
            patchItemParam04File("cmn/item_param_04.bin", 1212)

            patchExtendedTextBinFile("interface/nameentry/msg.bin", 1609)
            patchExtendedTextBinFile("interface/select/i_move.bin", 1675)

            patchTextBinFile("interface/charsel/svt_select.bin", 1369 - 1)
            patchTextBinFile("interface/cmn/dialog.bin", 1370 - 1)
            patchTextBinFile("interface/dayresult/d_result.bin", 1372 - 1)
            patchTextBinFile("interface/dungeon/i_dun_sysmsg.bin", 1373 - 1)
            patchTextBinFile("interface/equip/msg.bin", 1374 - 1)
            patchTextBinFile("interface/gameover/gov.bin", 1377 - 1)
            patchTextBinFile("interface/gradationair/i_ga.bin", 1378 - 1)
            patchTextBinFile("interface/infomatpop/msg.bin", 1392 - 1)
            patchTextBinFile("interface/infomatrixex/msg.bin", 1393 - 1)
            patchTextBinFile("interface/item/msg.bin", 1451 - 1)
            patchTextBinFile("interface/mainmenu/help.bin", 1452 - 1)
            patchTextBinFile("interface/modeselect/modeselect.bin", 1457 - 1)
            patchTextBinFile("interface/option/msg.bin", 1461 - 1)
            patchTextBinFile("interface/save/msg.bin", 1468 - 1)
            patchTextBinFile("interface/shop/msg.bin", 1477 - 1)
            patchTextBinFile("interface/status/msg.bin", 1489 - 1)
            patchTextBinFile("battle/interface/btl_msg.bin", 1490 - 1)
            patchTextBinFile("cmn/cmn_name.bin", 1496 - 1)

            patchInfomatrixFiles("interface/infomatrixex/infomatrix_alc_d_04.bin", 1683 - 1)
            patchInfomatrixFiles("interface/infomatrixex/infomatrix_ali_d_03.bin", 1710 - 1)
            patchInfomatrixFiles("interface/infomatrixex/infomatrix_eld_d_01.bin", 1737 - 1)
            patchInfomatrixFiles("interface/infomatrixex/infomatrix_emi_d_00.bin", 1764 - 1)
            patchInfomatrixFiles("interface/infomatrixex/infomatrix_fun_d_05.bin", 1791 - 1)
            patchInfomatrixFiles("interface/infomatrixex/infomatrix_gaw_d_07.bin", 1818 - 1)
            patchInfomatrixFiles("interface/infomatrixex/infomatrix_koo_d_06.bin", 1845 - 1)
            patchInfomatrixFiles("interface/infomatrixex/infomatrix_ner_d_00.bin", 1872 - 1)
            patchInfomatrixFiles("interface/infomatrixex/infomatrix_rob_d_02.bin", 1899 - 1)
            patchInfomatrixFiles("interface/infomatrixex/infomatrix_ryo_d_06.bin", 1926 - 1)
            patchInfomatrixFiles("interface/infomatrixex/infomatrix_tam_d_00.bin", 1953 - 1)
            patchInfomatrixFiles("interface/infomatrixex/infomatrix_war_d_04.bin", 1980 - 1)

            patchInDungeonFile("chr/emi/0000.dat", 2007 - 1, 102)
            patchInDungeonFile("chr/gat/0000.dat", 2109 - 1, 11)
            patchInDungeonFile("chr/kun/0000.dat", 2120 - 1, 11)
            patchInDungeonFile("chr/mal/0000.dat", 2131 - 1, 7)
            patchInDungeonFile("chr/ner/0000.dat", 2138 - 1, 106)
            patchInDungeonFile("chr/sin/0000.dat", 2244 - 1, 10)
            patchInDungeonFile("chr/tam/0000.dat", 2254 - 1, 96)
        }

        private fun patchSjisFile(path: String, translationOffset: Int, charset: Charset = Charsets.WINDOWS_932) {
            patchFile(path, translationOffset, charset, handler = { bytes, file, translation, offset, hCharset ->
                ExtraSjisFilePatcher(bytes, projectDir.child("src/chaDataTblJP.sjis").readBytes(),
                        file, translation, offset, hCharset)
            })
        }

        private fun patchItemParam01File(path: String, translationOffset: Int, charset: Charset = Charsets.WINDOWS_932) {
            patchFile(path, translationOffset, charset, ::ExtraItemParam01BinFilePatcher)
        }

        private fun patchItemParam04File(path: String, translationOffset: Int, charset: Charset = Charsets.WINDOWS_932) {
            patchFile(path, translationOffset, charset, ::ExtraItemParam04BinFilePatcher)
        }

        private fun patchTextBinFile(path: String, translationOffset: Int, charset: Charset = Charsets.WINDOWS_932) {
            patchFile(path, translationOffset, charset, ::ExtraIndexedTextBinFilePatcher)
        }

        private fun patchExtendedTextBinFile(path: String, translationOffset: Int, charset: Charset = Charsets.WINDOWS_932) {
            patchFile(path, translationOffset, charset, ::ExtraFixedSizeTextBinFilePatcher)
        }

        private fun patchInfomatrixFiles(path: String, translationOffset: Int, charset: Charset = Charsets.WINDOWS_932) {
            patchFile(path, translationOffset, charset, ::ExtraInfoMatrixBinDFilePatcher)
            patchFile(path.replace("_d_", "_t_"),
                    translationOffset, charset, ::ExtraInfoMatrixBinTFilePatcher)
        }

        private fun patchInDungeonFile(path: String, translationOffset: Int, count: Int, charset: Charset = Charsets.WINDOWS_932) {
            patchFile(path, translationOffset, charset, handler = { bytes, file, translation, offset, hCharset ->
                ExtraChrDatFilePatcher(bytes, file, translation, translationOrig, offset, count, hCharset)
            })
        }

        private fun patchFile(path: String, translationOffset: Int, charset: Charset = Charsets.WINDOWS_932,
                              handler: (ByteArray, File, ExtraTranslation, Int, Charset) -> Any?) {
            println("Patching $path...")
            val relPakPath = pakRefs.files[path]!!.first()
            val pakFile = cpkSrcDir.child(relPakPath)
            val pak = when (pakFile.extension) {
                "pak" -> PakFile(pakFile)
                "cmp" -> PakFile(CmpFile(pakFile).getData())
                else -> error("Unknown PAK extension")
            }
            val newOut = newPakSrcDir.child(path)
            newOut.parentFile.mkdirs()
            handler(pak.getEntry(path).bytes, newOut, translation, translationOffset, charset)
        }
    }

    data class TranslationEntry(val jp: String, val en: String, val note: String = "", val file: String, val fileOffset: Int)
}

