/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.compiler

import java.io.{ File => JFile }
import java.net.URLClassLoader
import java.nio.file.Files

import scala.reflect.io.Directory
import scala.reflect.io.Path.string2path
import scala.util.Try

import com.typesafe.zinc.{ Compiler, IncOptions, Inputs, Setup }
import org.slf4j.LoggerFactory
import xsbti.{ F0, Logger }
import xsbti.api.Compilation
import xsbti.compile.CompileOrder

import io.gatling.compiler.config.CompilerConfiguration
import io.gatling.compiler.config.ConfigUtils._

object ZincCompiler extends App {

  val configuration = CompilerConfiguration.configuration(args)

  private val logger = LoggerFactory.getLogger(getClass)
  private val FoldersToCache = List("bin", "conf", "user-files")
  private val compilerOptions = Seq(
    "-encoding",
    configuration.encoding,
    "-target:jvm-1.7",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:implicitConversions",
    "-language:postfixOps")

  Files.createDirectories(configuration.classesDirectory)

  val compilerClasspath = {
    val classLoader = Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader]
    classLoader.getURLs.map(_.toURI).map(new JFile(_))
  }

  def simulationInputs: Inputs = {

    val sources = Directory(configuration.simulationsDirectory.toString)
      .deepFiles
      .collect { case file if file.hasExtension("scala") || file.hasExtension("java") => file.jfile }
      .toSeq

      def analysisCacheMapEntry(directoryName: String) =
        (GatlingHome.toString / directoryName).jfile -> (configuration.binariesDirectory.toString / "cache" / directoryName).jfile

    Inputs.inputs(classpath = configuration.classpathElements,
      sources = sources,
      classesDirectory = configuration.classesDirectory.toFile,
      scalacOptions = compilerOptions,
      javacOptions = Nil,
      analysisCache = Some((configuration.binariesDirectory.toString / "zincCache").jfile),
      analysisCacheMap = FoldersToCache.map(analysisCacheMapEntry).toMap, // avoids having GATLING_HOME polluted with a "cache" folder
      forceClean = false,
      javaOnly = false,
      compileOrder = CompileOrder.Mixed,
      incOptions = IncOptions(nameHashing = true),
      outputRelations = None,
      outputProducts = None,
      mirrorAnalysis = false)
  }

  def setupZincCompiler: Setup = {
      def jarMatching(classpath: Seq[JFile], regex: String): JFile =
        classpath
          .find(file => !file.getName.startsWith(".") && regex.r.findFirstMatchIn(file.getName).isDefined)
          .getOrElse(throw new RuntimeException(s"Can't find the jar matching $regex"))

    val scalaCompiler = jarMatching(configuration.classpathElements, """scala-compiler.*\.jar$""")
    val scalaLibrary = jarMatching(configuration.classpathElements, """scala-library.*\.jar$""")
    val scalaReflect = jarMatching(configuration.classpathElements, """scala-reflect.*\.jar$""")
    val sbtInterfaceSrc: JFile = new JFile(classOf[Compilation].getProtectionDomain.getCodeSource.getLocation.toURI)
    val compilerInterfaceSrc: JFile = jarMatching(compilerClasspath, """compiler-interface-.*-sources.jar$""")

    Setup.setup(scalaCompiler = scalaCompiler,
      scalaLibrary = scalaLibrary,
      scalaExtra = List(scalaReflect),
      sbtInterface = sbtInterfaceSrc,
      compilerInterfaceSrc = compilerInterfaceSrc,
      javaHomeDir = None,
      forkJava = false)
  }

  // Setup the compiler
  val setup = setupZincCompiler
  val zincLogger = new Logger {
    def error(arg: F0[String]): Unit = logger.error(arg.apply)
    def warn(arg: F0[String]): Unit = logger.warn(arg.apply)
    def info(arg: F0[String]): Unit = logger.info(arg.apply)
    def debug(arg: F0[String]): Unit = logger.debug(arg.apply)
    def trace(arg: F0[Throwable]): Unit = logger.trace("", arg.apply)
  }

  val zincCompiler = Compiler.create(setup, zincLogger)

  // Define the inputs
  val inputs = simulationInputs
  Inputs.debug(inputs, zincLogger)

  if (Try(zincCompiler.compile(inputs)(zincLogger)).isFailure) {
    // Zinc is already logging all the issues, no need to deal with the exception.
    System.exit(1)
  }
}
