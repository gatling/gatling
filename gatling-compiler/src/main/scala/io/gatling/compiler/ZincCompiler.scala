/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.compiler

import java.io.{ File => JFile }
import java.net.{ URL, URLClassLoader }
import java.nio.file.Files
import java.util.jar.{ Manifest => JManifest, Attributes }

import scala.collection.JavaConversions._
import scala.reflect.io.Directory
import scala.util.{ Failure, Try }

import com.typesafe.zinc.{ Compiler, IncOptions, Inputs, Setup }
import org.slf4j.LoggerFactory
import xsbti.{ F0, Logger }
import xsbti.api.Compilation
import xsbti.compile.CompileOrder

import io.gatling.compiler.config.CompilerConfiguration
import io.gatling.compiler.config.ConfigUtils._

object ZincCompiler extends App {

  private val configuration = CompilerConfiguration.configuration(args)

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
    "-language:postfixOps"
  )

  Files.createDirectories(configuration.binariesDirectory)

  private val compilerClasspath = {
    val classLoader = Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader]
    val files = classLoader.getURLs.map(url => new JFile(url.toURI))

    if (files.exists(_.getName.startsWith("gatlingbooter"))) {
      // yippee, we've been started by the manifest-only jar,
      // we have to add the manifest Class-Path entries

      val manifests = Thread.currentThread.getContextClassLoader.getResources("META-INF/MANIFEST.MF")
        .map { url =>
          val is = url.openStream()
          try {
            new JManifest(is)
          } finally {
            is.close()
          }
        }

      val classPathEntries = manifests.collect {
        case manifest if Option(manifest.getMainAttributes.getValue(Attributes.Name.MAIN_CLASS)) == Some("io.gatling.mojo.MainWithArgsInFile") =>
          manifest.getMainAttributes.getValue(Attributes.Name.CLASS_PATH).split(" ").map(url => new JFile(new URL(url).toURI))
      }

      files ++ classPathEntries.flatten

    } else {
      files
    }
  }

  private def simulationInputs: Inputs = {

    val sources = Directory(configuration.simulationsDirectory.toString)
      .deepFiles
      .collect { case file if file.hasExtension("scala") || file.hasExtension("java") => file.jfile }
      .toSeq

      def analysisCacheMapEntry(directoryName: String) =
        (GatlingHome / directoryName).toFile -> (configuration.binariesDirectory / "cache" / directoryName).toFile

    Inputs.inputs(
      classpath = configuration.classpathElements,
      sources = sources,
      classesDirectory = configuration.binariesDirectory.toFile,
      scalacOptions = compilerOptions,
      javacOptions = Nil,
      analysisCache = Some((configuration.binariesDirectory / "zincCache").toFile),
      analysisCacheMap = FoldersToCache.map(analysisCacheMapEntry).toMap, // avoids having GATLING_HOME polluted with a "cache" folder
      forceClean = false,
      javaOnly = false,
      compileOrder = CompileOrder.Mixed,
      incOptions = IncOptions(),
      outputRelations = None,
      outputProducts = None,
      mirrorAnalysis = false
    )
  }

  private def setupZincCompiler: Setup = {
      def jarMatching(classpath: Seq[JFile], regex: String): JFile =
        classpath
          .find(file => !file.getName.startsWith(".") && regex.r.findFirstMatchIn(file.getName).isDefined)
          .getOrElse(throw new RuntimeException(s"Can't find the jar matching $regex"))

    val scalaCompiler = jarMatching(configuration.classpathElements, """scala-compiler.*\.jar$""")
    val scalaLibrary = jarMatching(configuration.classpathElements, """scala-library.*\.jar$""")
    val scalaReflect = jarMatching(configuration.classpathElements, """scala-reflect.*\.jar$""")
    val sbtInterfaceSrc: JFile = new JFile(classOf[Compilation].getProtectionDomain.getCodeSource.getLocation.toURI)
    val compilerInterfaceSrc: JFile = jarMatching(compilerClasspath, """compiler-interface-.*-sources.jar$""")

    Setup.setup(
      scalaCompiler = scalaCompiler,
      scalaLibrary = scalaLibrary,
      scalaExtra = List(scalaReflect),
      sbtInterface = sbtInterfaceSrc,
      compilerInterfaceSrc = compilerInterfaceSrc,
      javaHomeDir = None,
      forkJava = false
    )
  }

  // Setup the compiler
  private val setup = setupZincCompiler

  private val zincLogger = new Logger {
    def error(arg: F0[String]): Unit = logger.error(arg.apply)
    def warn(arg: F0[String]): Unit = logger.warn(arg.apply)
    def info(arg: F0[String]): Unit = logger.info(arg.apply)
    def debug(arg: F0[String]): Unit = logger.debug(arg.apply)
    def trace(arg: F0[Throwable]): Unit = logger.trace("", arg.apply)
  }

  private val zincCompiler = Compiler.create(setup, zincLogger)

  // Define the inputs
  private val inputs = simulationInputs
  Inputs.debug(inputs, zincLogger)

  Try(zincCompiler.compile(inputs)(zincLogger)) match {
    case Failure(t) =>
      logger.error("Compilation crashed", t)
      System.exit(1)
    case _ =>
      logger.debug("Compilation successful")
    // Zinc is already logging all the issues, no need to deal with the exception.
  }
}
