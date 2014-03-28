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
package io.gatling.app

import java.io.{ File => JFile }
import java.net.URLClassLoader

import scala.tools.nsc.io.Directory
import scala.tools.nsc.io.Path.string2path
import scala.util.Try

import com.typesafe.scalalogging.slf4j.StrictLogging
import com.typesafe.zinc.{ Compiler, IncOptions, Inputs, Setup }

import xsbti.{ F0, Logger }
import xsbti.api.Compilation
import xsbti.compile.CompileOrder

object ZincCompiler extends StrictLogging {

  private val foldersToCache = List("bin", "conf", "user-files")

  def main(args: Array[String]): Unit = {

    val gatlingHome = args(0)
    val sourceDirectory = Directory(args(1))
    val binDirectory = args(2)
    val classesDirectory = args(3)
    val encoding = args(4)

    val compilerOptions = Seq(
      "-encoding",
      encoding,
      "-target:jvm-1.6",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-language:postfixOps")

    val classpathURLs = Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader].getURLs

      def simulationInputs: Inputs = {
        val classpath = classpathURLs.map(url => new JFile(url.toURI))

        val sources = sourceDirectory
          .deepFiles
          .collect { case file if file.hasExtension("scala") => file.jfile }
          .toSeq

          def analysisCacheMapEntry(directoryName: String) =
            (gatlingHome / directoryName).jfile -> (binDirectory / "cache" / directoryName).jfile

        Inputs.inputs(classpath = classpath,
          sources = sources,
          classesDirectory = classesDirectory.jfile,
          scalacOptions = compilerOptions,
          javacOptions = Nil,
          analysisCache = Some((binDirectory / "zincCache").jfile),
          analysisCacheMap = foldersToCache.map(analysisCacheMapEntry).toMap, // avoids having GATLING_HOME polluted with a "cache" folder
          forceClean = false,
          javaOnly = false,
          compileOrder = CompileOrder.JavaThenScala,
          incOptions = IncOptions(),
          outputRelations = None,
          outputProducts = None,
          mirrorAnalysis = false)
      }

      def setupZincCompiler: Setup = {
          def jarMatching(regex: String): JFile = {
            val compiledRegex = regex.r
            val jarUrl = classpathURLs
              .find(url => compiledRegex.findFirstMatchIn(url.toString).isDefined)
              .getOrElse(throw new RuntimeException(s"Can't find the jar matching $regex"))

            new JFile(jarUrl.toURI)
          }

        val scalaCompiler = jarMatching("""(.*scala-compiler.*\.jar)$""")
        val scalaLibrary = jarMatching("""(.*scala-library.*\.jar)$""")
        val scalaReflect = jarMatching("""(.*scala-reflect.*\.jar)$""")
        val sbtInterfaceSrc: JFile = new JFile(classOf[Compilation].getProtectionDomain.getCodeSource.getLocation.toURI)
        val compilerInterfaceSrc: JFile = jarMatching("""(.*compiler-interface-.*-sources.jar)$""")

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
}
