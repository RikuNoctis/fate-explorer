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
import com.github.rvesse.airline.annotations.Command
import com.github.rvesse.airline.annotations.Option
import com.github.rvesse.airline.annotations.OptionType
import com.github.rvesse.airline.help.Help
import com.github.rvesse.airline.parser.errors.ParseException
import com.kotcrab.fate.util.getJarPath
import java.io.File
import kotlin.system.exitProcess


/** @author Kotcrab */
object FateCli {
    @JvmStatic
    fun main(args: Array<String>) {
        val builder = Cli.builder<Runnable>("fate")
                .withDescription("Extra, CCC and Extella toolkit")
                .withDefaultCommand(Help::class.java)

        val extraGroup = builder.withGroup("extra")
                .withDescription("Utilities for Fate/Extra")

        extraGroup.withSubGroup("dump")
                .withDescription("Dump specified file to standard output")
                .withCommands(TestCmd::class.java)

        stdCliLauncher(builder.build(), args)
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

@Command(name = "test", description = "Test command")
class TestCmd : FateCommand() {
    override fun run() {
        println("Test")
    }
}

abstract class FateCommand : Runnable {
    @Option(type = OptionType.GLOBAL, name = ["--stacktrace"], description = "Show error full stacktrace")
    val showErrorStacktrace = false
    protected val baseDir: File by lazy {
        File(getJarPath(FateCommand::class.java)).parentFile
    }

    fun abort(msg: String): Nothing {
        println(msg)
        exitProcess(0)
    }
}
