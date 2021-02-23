/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
import java.nio.file.{ Files, Path }
import java.util.Optional
import java.util.jar.{ Attributes, Manifest => JManifest }

import scala.jdk.CollectionConverters._
import scala.reflect.io.Directory

import io.gatling.compiler.config.CompilerConfiguration
import io.gatling.compiler.config.ConfigUtils._

import org.slf4j.LoggerFactory

import sbt.internal.inc._
import sbt.internal.inc.classpath.ClasspathUtil
import sbt.util.{ Level, Logger => SbtLogger }
import sbt.util.ShowLines._
import xsbti.{ FileConverter, Position, Problem, Reporter, T2, VirtualFile }
import xsbti.compile.{ FileAnalysisStore => _, ScalaInstance => _, _ }
import xsbti.compile.analysis.ReadStamps

object ZincCompiler extends App with ProblemStringFormats {

  private val logger = LoggerFactory.getLogger(getClass)

  private def manifestClasspath: Array[JFile] = {

    val manifests = Thread.currentThread.getContextClassLoader
      .getResources("META-INF/MANIFEST.MF")
      .asScala
      .map { url =>
        val is = url.openStream()
        try {
          new JManifest(is)
        } finally {
          is.close()
        }
      }

    val classPathEntries = manifests.collect {
      case manifest
          if Option(
            manifest.getMainAttributes.getValue(Attributes.Name.MAIN_CLASS)
          ).contains("io.gatling.mojo.MainWithArgsInFile") =>
        manifest.getMainAttributes
          .getValue(Attributes.Name.CLASS_PATH)
          .split(" ")
          .map(url => new JFile(new URL(url).toURI))
    }

    classPathEntries.flatten.toArray
  }

  private def jarMatching(classpath: Array[JFile], regex: String): JFile =
    classpath
      .find(file =>
        !file.getName
          .startsWith(".") && regex.r.findFirstMatchIn(file.getName).isDefined
      )
      .getOrElse(
        throw new RuntimeException(s"Can't find the jar matching $regex")
      )

  private def doCompile(): Unit = {
    val configuration = CompilerConfiguration.configuration(args)
    Files.createDirectories(configuration.binariesDirectory)

    val classpath: Array[JFile] = {
      val files = System
        .getProperty("java.class.path")
        .split(JFile.pathSeparator)
        .map(new JFile(_))

      if (files.exists(_.getName.startsWith("gatlingbooter"))) {
        // we've been started by the manifest-only jar,
        // we have to switch the manifest Class-Path entries
        manifestClasspath
      } else {
        files
      }
    }

    val scalaLibraryJar = jarMatching(classpath, """scala-library-.*\.jar$""")
    val scalaReflectJar = jarMatching(classpath, """scala-reflect-.*\.jar$""")
    val scalaCompilerJar = jarMatching(classpath, """scala-compiler-.*\.jar$""")
    val allScalaJars = Array(scalaCompilerJar, scalaLibraryJar, scalaReflectJar)

    val compilerBridgeJar =
      jarMatching(classpath, """compiler-bridge_.*\.jar$""")
    val cacheFile = (GatlingHome / "target" / "inc_compile.zip").toFile

    val scalaVersionExtractor = """scala-library-(.*)\.jar$""".r

    val scalaVersion = scalaLibraryJar.getName match {
      case scalaVersionExtractor(version) => version
    }

    val scalaInstance =
      new ScalaInstance(
        version = scalaVersion,
        loader = new URLClassLoader(allScalaJars.map(_.toURI.toURL)),
        loaderLibraryOnly = ClasspathUtil.rootLoader,
        libraryJar = scalaLibraryJar,
        compilerJar = scalaCompilerJar,
        allJars = allScalaJars,
        explicitActual = Some(scalaVersion)
      )

    val sbtLogger = new SbtLogger {
      override def trace(t: => Throwable): Unit =
        logger.debug(Option(t.getMessage).getOrElse("error"), t)

      override def success(message: => String): Unit =
        logger.info(s"Success: $message")

      override def log(level: Level.Value, message: => String): Unit =
        level match {
          case Level.Error =>
            if (message.startsWith("## Exception when compiling")) {
              // see IncrementalCompilerImpl.handleCompilationError
              // Exception with stacktrace will be thrown and logged properly below in try/catch block
              logger.error(message.substring(0, message.indexOf("\n")))
            } else {
              logger.error(message)
            }
          case Level.Warn  => logger.warn(message)
          case Level.Info  => logger.info(message)
          case Level.Debug => logger.debug(message)
        }
    }

    val compiler = ZincUtil.defaultIncrementalCompiler

    val scalaCompiler = new AnalyzingCompiler(
      scalaInstance = scalaInstance,
      provider = ZincCompilerUtil
        .constantBridgeProvider(scalaInstance, compilerBridgeJar),
      classpathOptions = ClasspathOptionsUtil.auto,
      onArgsHandler = _ => (),
      classLoaderCache = None
    )

    val compilers = ZincUtil.compilers(
      scalaInstance,
      ClasspathOptionsUtil.boot,
      None,
      scalaCompiler
    )

    val lookup = new PerClasspathEntryLookup {
      override def analysis(
          classpathEntry: VirtualFile
      ): Optional[CompileAnalysis] = Optional.empty[CompileAnalysis]

      override def definesClass(classpathEntry: VirtualFile): DefinesClass =
        Locate.definesClass(classpathEntry)
    }

    val maxErrors = 100

    val reporter = new LoggedReporter(maxErrors, sbtLogger) {
      override protected def logError(problem: Problem): Unit =
        logger.error(problem.lines.mkString("\n"))
      override protected def logWarning(problem: Problem): Unit =
        logger.warn(problem.lines.mkString("\n"))
      override protected def logInfo(problem: Problem): Unit =
        logger.info(problem.lines.mkString("\n"))
    }

    val setup =
      Setup.of(
        lookup, // _perClasspathEntryLookup
        false, // _skip
        cacheFile, // _cacheFile
        CompilerCache.fresh, // _cache
        IncOptions.of(), // _incrementalCompilerOptions
        reporter, // _reporter
        Optional.empty[CompileProgress], // _progress
        Array.empty[T2[String, String]] // _extra
      )

    val sources: Array[JFile] =
      Directory(configuration.simulationsDirectory.toString).deepFiles.collect {
        case file if file.hasExtension("scala") || file.hasExtension("java") =>
          file.jfile
      }.toArray

    val analysisStore =
      AnalysisStore.getCachedStore(FileAnalysisStore.binary(cacheFile))

    val previousResult = {
      val analysisContents = analysisStore.get
      if (analysisContents.isPresent) {
        val analysisContents0 = analysisContents.get
        val previousAnalysis = analysisContents0.getAnalysis
        val previousSetup = analysisContents0.getMiniSetup
        PreviousResult.of(
          Optional.of(previousAnalysis),
          Optional.of(previousSetup)
        )
      } else {
        PreviousResult.of(
          Optional.empty[CompileAnalysis],
          Optional.empty[MiniSetup]
        )
      }
    }

    val options = CompileOptions.of(
      (classpath :+ configuration.binariesDirectory.toFile).map(file => new PlainVirtualFile(file.toPath): VirtualFile), // _classpath
      sources
        .map(file => new PlainVirtualFile(file.toPath): VirtualFile), // _sources
      configuration.binariesDirectory, // _classesDirectory
      Array(
        "-encoding",
        configuration.encoding,
        "-target:jvm-1.8",
        "-deprecation",
        "-feature",
        "-unchecked",
        "-language:implicitConversions",
        "-language:postfixOps"
      ) ++ configuration.extraScalacOptions, // _scalacOptions
      Array.empty[String], // _javacOptions
      100, // _maxErrors
      (position: Position) => position, // _sourcePositionMapper
      CompileOrder.Mixed, // _order
      Optional.empty[Path], // _temporaryClassesDirectory
      Optional
        .of(PlainVirtualFileConverter.converter: FileConverter), // _converter
      Optional.empty[ReadStamps], // _stamper
      Optional.empty[Output] // _earlyOutput
    )

    val inputs = Inputs.of(compilers, options, setup, previousResult)

    val newResult = compiler.compile(inputs, sbtLogger)
    analysisStore.set(
      AnalysisContents.create(newResult.analysis(), newResult.setup())
    )
  }

  try {
    doCompile()
    logger.debug("Compilation successful")
  } catch {
    case t: Throwable =>
      logger.error("Compilation crashed", t)
      System.exit(1)
  }
}
