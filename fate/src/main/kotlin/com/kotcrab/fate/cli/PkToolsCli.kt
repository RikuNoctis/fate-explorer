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

import com.github.rvesse.airline.annotations.Arguments
import com.github.rvesse.airline.annotations.Cli
import com.github.rvesse.airline.annotations.Command
import com.github.rvesse.airline.annotations.Option
import com.github.rvesse.airline.annotations.restrictions.Required
import com.github.rvesse.airline.help.Help
import com.kotcrab.fate.file.extella.PkFile
import java.io.File

/** @author Kotcrab */
@Cli(
    name = "pktools", description = "Allows to manipulate PK archives",
    defaultCommand = Help::class,
    commands = [(Extract::class), (ExtractAll::class), (ExtractDnD::class)]
)
object PkToolsCli {
    @JvmStatic
    fun main(args: Array<String>) {
        FateCli.stdCliLauncher(PkToolsCli::class.java, args)
    }
}

@Command(name = "extract", description = "Extracts .pk archive")
class Extract : PkToolsCommand() {
    @Required
    @Arguments(description = "PK archive path")
    private val pkPath = ""
    @Option(
        name = ["--output", "-o"],
        description = "Output directory, must exist and be empty. If not specified default will be used"
    )
    private val outputPath = ""

    override fun run() {
        extractPk(pkPath, outputPath)
    }
}

@Command(name = "extractDnD", hidden = true)
class ExtractDnD : PkToolsCommand() {
    @Arguments()
    private val pkPath = ""

    override fun run() {
        if (pkPath.isEmpty()) abort("You must drag-and-drop PK archive on this file to unpack it")
        extractPk(pkPath, "")
    }
}

@Command(
    name = "extractAll",
    description = "Extract all .pk files from specified directory. Default output directory is used."
)
class ExtractAll : PkToolsCommand() {
    @Required
    @Arguments(description = "Source folder containing .pk files")
    private val srcFolder = ""

    override fun run() {
        val src = File(srcFolder)
        if (srcFolder.isEmpty() || src.exists() == false || src.list().isEmpty()) {
            abort("Source folder does not exists or is empty")
        }

        val pkFiles = src.listFiles().filter { it.extension == "pk" }

        var count = 0
        var failedCount = 0
        pkFiles.forEach { pkFile ->
            try {
                val out = getOutputDirectory(pkFile.nameWithoutExtension)
                println("Extracting: ${pkFile.absolutePath}")
                println("Output folder: ${out.absolutePath}")
                if (out.listFiles().isNotEmpty()) {
                    println("Output folder with this archive name already exist: ${pkFile.nameWithoutExtension}")
                    failedCount++
                    return@forEach
                }
                PkFile(pkFile, out)
                count++
            } catch (e: IllegalStateException) {
                println("Error extracting archive")
                e.printStackTrace()
                failedCount++
            }
        }
        if (failedCount == 0) {
            println("Finished extracting $count archives")
        } else {
            println("Finished extracting $count archives ($failedCount failed)")
        }
    }
}

abstract class PkToolsCommand : FateCommand() {
    fun extractPk(pkPath: String, outPath: String) {
        val pkFile = File(pkPath)
        if (pkFile.exists() == false) abort("File does not exist")
        if (pkFile.extension != "pk") abort("File must have .pk extension")
        val out: File

        if (outPath.isEmpty()) {
            out = getOutputDirectory(pkFile.nameWithoutExtension)
            if (out.listFiles().isNotEmpty()) {
                abort("Output folder with this archive name already exist: ${pkFile.nameWithoutExtension}")
            }
        } else {
            out = File(outPath)
            if (out.exists() == false) abort("Specified output directory does not exist")
            if (out.listFiles().isNotEmpty()) abort("Specified output directory is not empty")
        }
        println("Extracting: ${pkFile.absolutePath}")
        println("Output folder: ${out.absolutePath}")
        PkFile(pkFile, out)
    }
}
