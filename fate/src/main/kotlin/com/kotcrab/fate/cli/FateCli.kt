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

package com.kotcrab.fate.cli

import com.github.rvesse.airline.Cli
import com.github.rvesse.airline.annotations.Arguments
import com.github.rvesse.airline.annotations.Command
import com.github.rvesse.airline.annotations.Option
import com.github.rvesse.airline.annotations.OptionType
import com.github.rvesse.airline.annotations.restrictions.Required
import com.github.rvesse.airline.help.Help
import com.github.rvesse.airline.parser.errors.ParseException
import com.kotcrab.fate.PackageExtractor
import com.kotcrab.fate.file.*
import com.kotcrab.fate.file.extra.ExtraInfoMatrixBinFile
import com.kotcrab.fate.file.extra.ExtraItemParam01BinFile
import com.kotcrab.fate.file.extra.ExtraItemParam04BinFile
import com.kotcrab.fate.patcher.CmpEncoder
import com.kotcrab.fate.patcher.PakWriter
import kio.util.*
import java.io.File
import kotlin.reflect.KClass
import kotlin.system.exitProcess

/** @author Kotcrab */
object FateCli {
    @JvmStatic
    fun main(args: Array<String>) {
        val builder = Cli.builder<Runnable>("fate")
                .withDescription("Extra, CCC and Extella toolkit")
                .withDefaultCommand(Help::class.java)

        builder.withGroup("extract")
                .withDescription("Utilities for extracting archives")
                .withCommands(cmds(ExtractCpk::class, ExtractPak::class, ExtractCmp::class, ExtractExtraPackage::class))

        builder.withGroup("create")
                .withDescription("Utilities for converting files into Fate formats")
                .withCommands(cmds(CreateCmp::class, CreatePak::class))

        builder.withGroup("dump")
                .withDescription("Dump files to standard output in JSON format. " +
                        "Should work for Extra US, Extra JP and CCC.")
                .withCommands(cmds(DumpIndexedTextBin::class, DumpFixedSizeTextBin::class, DumpSjisBin::class))

        val extraGroup = builder.withGroup("extra")
                .withDescription("Utilities for Fate/Extra (US version)")
        extraGroup.withSubGroup("dump")
                .withDescription("Dump files to standard output in JSON format")
                .withCommands(cmds(DumpExtraInfoMatrix::class, DumpExtraItemParam01::class, DumpExtraItemParam04::class))

        stdCliLauncher(builder.build(), args)
    }

    private fun cmds(vararg classes: KClass<out Runnable>): List<Class<out Runnable>> {
        return classes.map { it.java }
    }

    fun stdCliLauncher(cliClass: Class<*>, args: Array<String>) {
        stdCliLauncher(Cli(cliClass), args)
    }

    fun stdCliLauncher(cli: Cli<Runnable>, args: Array<String>) {
        var cmd: Runnable? = null
        try {
            cmd = cli.parse(*args)
            cmd.run()
        } catch (e: ParseException) {
            println(e.message)
        } catch (e: Exception) {
            if (e.message == null || cmd !is PkToolsCommand || cmd.showErrorStacktrace) {
                e.printStackTrace()
            } else {
                println("Error: ${e.message}")
                println("Run with --stacktrace to get the full stack trace")
            }
        }
    }
}

@Command(name = "cmp", description = "Create .cmp file from a .pak file")
class CreateCmp : FateCommand() {
    @Required
    @Arguments(description = "Path to input .pak file")
    private val pakPath = ""

    @Option(name = arrayOf("--second-pass"), description = "Try to get smaller file size by searching entire dictionary (slower)")
    private val secondPass = false

    @Option(name = arrayOf("--output", "-o"), description = "Output file")
    private val outputPath = "output.cmp"

    override fun run() {
        val pak = File(pakPath)
        if (pak.exists() == false) abort(".pak file does not exist")
        val output = File(outputPath)
        if (output.exists()) abort("Output file already exists: $outputPath")
        val compressed = CmpEncoder(pak, secondPass).getData()
        output.writeBytes(compressed)
    }
}

@Command(name = "cpk", description = "Extracts CPK archive")
class ExtractCpk : FateCommand() {
    @Required
    @Arguments(description = "Path to input CPK")
    private val cpkPath = ""

    @Option(name = arrayOf("--output", "-o"), description = "Output directory")
    private val outputPath: String? = null

    override fun run() {
        val cpk = File(cpkPath)
        if (cpk.exists() == false) abort("CPK file does not exist")
        val output = if (outputPath == null) {
            getOutputDirectory(cpk.nameWithoutExtension)
        } else {
            File(outputPath)
        }
        if (!output.exists()) abort("Output directory does not exist: ${output.absolutePath}")
        if (output.listFiles().isNotEmpty()) abort("Output directory is not empty: ${output.absolutePath}")
        CpkFile(cpk).extractTo(output)
    }
}

@Command(name = "pak", description = "Extracts PAK archive")
class ExtractPak : FateCommand() {
    @Required
    @Arguments(description = "Path to input PAK")
    private val pakPath = ""

    @Option(name = arrayOf("--output", "-o"), description = "Output directory")
    private val outputPath: String? = null

    override fun run() {
        val pak = File(pakPath)
        if (pak.exists() == false) abort("PAK file does not exist")
        val output = if (outputPath == null) {
            getOutputDirectory(pak.nameWithoutExtension)
        } else {
            File(outputPath)
        }
        if (!output.exists()) abort("Output directory does not exist: ${output.absolutePath}")
        if (output.listFiles().isNotEmpty()) abort("Output directory is not empty: ${output.absolutePath}")
        PakFile(pak).entries.forEach {
            println("Extract ${it.path}")
            val outFile = output.child(it.path)
            outFile.parentFile.mkdirs()
            outFile.writeBytes(it.bytes)
        }
    }
}

@Command(name = "cmp", description = "Extracts CMP archive")
class ExtractCmp : FateCommand() {
    @Required
    @Arguments(description = "Path to input CMP")
    private val cmpPath = ""

    @Option(name = arrayOf("--output", "-o"), description = "Output directory")
    private val outputPath: String? = null

    @Option(name = arrayOf("--decompress-only", "-d"), description = "Do not attempt to automatically" +
            "extract decompressed CMP as PAK file")
    private val decompressOnly = false

    override fun run() {
        val cmp = File(cmpPath)
        if (cmp.exists() == false) abort("CMP file does not exist")
        val output = if (outputPath == null) {
            getOutputDirectory(cmp.nameWithoutExtension)
        } else {
            File(outputPath)
        }
        if (!output.exists()) abort("Output directory does not exist: ${output.absolutePath}")
        if (decompressOnly) {
            println("Decompress ${cmp.name}")
            val outputFile = output.child("${cmp.nameWithoutExtension}_decompressed.cmp")
            if (outputFile.exists()) abort("Output file already exists: ${outputFile.absolutePath}")
            CmpFile(cmp).writeToFile(outputFile.absolutePath)
        } else {
            if (output.listFiles().isNotEmpty()) abort("Output directory is not empty: ${output.absolutePath}")
            PakFile(CmpFile(cmp).getData()).entries.forEach {
                println("Extract ${it.path}")
                val outFile = output.child(it.path)
                outFile.parentFile.mkdirs()
                outFile.writeBytes(it.bytes)
            }
        }
    }
}

@Command(name = "package", description = "Performs batch extraction on all .CMP and .PAK files from specified directory" +
        "This should be used to extract all Fate/Extra or Fate/Extra CCC files from internal archives.")
class ExtractExtraPackage : FateCommand() {
    @Required
    @Arguments(description = "Path to source directory")
    private val srcPath = ""

    @Option(name = arrayOf("--output", "-o"), description = "Output directory")
    private val outputPath: String? = null

    override fun run() {
        val srcDir = File(srcPath)
        if (srcDir.exists() == false) abort("Source direcotry does not exist")
        val output = if (outputPath == null) {
            getOutputDirectory("fate-extra-package")
        } else {
            File(outputPath)
        }
        if (!output.exists()) abort("Output directory does not exist: ${output.absolutePath}")
        if (output.listFiles().isNotEmpty()) abort("Output directory is not empty: ${output.absolutePath}")
        PackageExtractor(srcDir, output)
    }
}

@Command(name = "pak", description = "Create .pak file from a directory of files")
class CreatePak : FateCommand() {
    @Required
    @Arguments(description = "Path to input directory")
    private val dirPath = ""

    @Option(name = arrayOf("--output", "-o"), description = "Output file")
    private val outputPath = "output.pak"

    override fun run() {
        val dir = File(dirPath)
        if (dir.exists() == false || dir.list().isEmpty()) abort("input directory does not exist or it's empty")
        val output = File(outputPath)
        if (output.exists()) abort("Output file already exists: $outputPath")
        var count = 0
        val entries = mutableListOf<PakFileEntry>()
        walkDir(dir, processFile = { file ->
            val relPath = file.relativizePath(dir)
            if (relPath.length > 0x40) error("File path is too long (maximum 64 characters): $relPath")
            entries.add(PakFileEntry(count++, relPath, file.readBytes()))
        })
        PakWriter(entries, output)
    }
}

@Command(name = "infomatrix", description = "Dumps infomatrix data")
class DumpExtraInfoMatrix : FateCommand() {
    @Required
    @Arguments(title = ["dFilePath", "tFilePath"], description = "Path of infomatrix_---_d_--.bin and " +
            "infomatrix_---_t_--.bin files")
    private val filePaths: List<String> = mutableListOf()

    override fun run() {
        if (filePaths.size != 2) abort("You must specify path to d and t infomatrix files")
        val dFile = File(filePaths[0])
        val tFile = File(filePaths[1])
        if (dFile.exists() == false) abort("Infomatrix d file does not exist")
        if (tFile.exists() == false) abort("Infomatrix t file does not exist")
        println(stdGson.toJson(ExtraInfoMatrixBinFile(dFile, tFile).entries))
    }
}

@Command(name = "itemparam01", description = "Dumps itemparam01 data")
class DumpExtraItemParam01 : FateCommand() {
    @Required
    @Arguments(description = "Path to item_param_01.bin file")
    private val filePath = ""

    override fun run() {
        val file = File(filePath)
        if (file.exists() == false) abort("itemparam01 file does not exist")
        println(stdGson.toJson(ExtraItemParam01BinFile(file, false).entries))
    }
}

@Command(name = "itemparam04", description = "Dumps itemparam04 data")
class DumpExtraItemParam04 : FateCommand() {
    @Required
    @Arguments(description = "Path to item_param_04.bin file")
    private val filePath = ""

    override fun run() {
        val file = File(filePath)
        if (file.exists() == false) abort("itemparam04 file does not exist")
        println(stdGson.toJson(ExtraItemParam04BinFile(file, false).entries))
    }
}

@Command(name = "indexed-text-bin", description = "Dumps indexed text bin files")
class DumpIndexedTextBin : FateCommand() {
    @Required
    @Arguments(description = "Path to bin file")
    private val filePath = ""

    override fun run() {
        val file = File(filePath)
        if (file.exists() == false) abort("file does not exist")
        println(stdGson.toJson(IndexedTextBinFile(file).entries))
    }
}

@Command(name = "fixedsize-text-bin", description = "Dumps fixed size text bin files")
class DumpFixedSizeTextBin : FateCommand() {
    @Required
    @Arguments(description = "Path to bin file")
    private val filePath = ""

    override fun run() {
        val file = File(filePath)
        if (file.exists() == false) abort("file does not exist")
        println(stdGson.toJson(FixedSizeTextBinFile(file).entries))
    }
}

@Command(name = "sjis", description = "Dumps .sjis file")
class DumpSjisBin : FateCommand() {
    @Required
    @Arguments(description = "Path to sjis file")
    private val filePath = ""

    override fun run() {
        val file = File(filePath)
        if (file.exists() == false) abort("file does not exist")
        println(stdGson.toJson(SjisFile(file).entries))
    }
}

abstract class FateCommand : Runnable {
    @Option(type = OptionType.GLOBAL, name = ["--stacktrace"], description = "Show error full stacktrace")
    val showErrorStacktrace = false
    protected val baseDir: File by lazy {
        File(getJarPath(FateCommand::class.java)).parentFile
    }

    fun getOutputDirectory(filename: String): File {
        val outBase = File(baseDir, "out")
        if (!outBase.exists()) outBase.mkdir()
        val out = outBase.child(filename)
        out.mkdir()
        return out
    }

    fun abort(msg: String): Nothing {
        println(msg)
        exitProcess(1)
    }
}
