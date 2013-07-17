/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.tools.nsc.io.{ Directory, Path }
import scala.tools.nsc.io.Path.{ jfile2path, string2path }
import scala.util.Try

import com.typesafe.scalalogging.slf4j.Logging
import com.typesafe.zinc.{ Compiler, Inputs, Setup }

import sbt.inc.IncOptions
import xsbti.Logger
import xsbti.api.Compilation
import xsbti.compile.CompileOrder

object ZincCompiler extends Logging {

	def main(args: Array[String]) {

		val gatlingHome = args(0)
		val sourceDirectory = Directory(args(1))
		val binDirectory = args(2)
		val classesDirectory = args(3)
		val encoding = args(4)

		val classpathURLs = Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader].getURLs

		def simulationInputs = {
			val classpath = classpathURLs.map(url => new JFile(url.toURI))

			val sources = sourceDirectory
				.deepFiles
				.collect { case file if (file.hasExtension("scala")) => file.jfile }
				.toSeq

			def analysisCacheMapEntry(directoryName: String) = (gatlingHome / directoryName).jfile -> (binDirectory / "cache" / directoryName).jfile

			Inputs.inputs(classpath = classpath,
				sources = sources,
				classesDirectory = classesDirectory.jfile,
				scalacOptions = Seq("-encoding", encoding, "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-language:implicitConversions", "-language:reflectiveCalls", "-language:postfixOps"),
				javacOptions = Nil,
				analysisCache = Some((binDirectory / "zincCache").jfile),
				analysisCacheMap = Map(analysisCacheMapEntry("bin"), analysisCacheMapEntry("conf"), analysisCacheMapEntry("user-files")), // avoids having GATLING_HOME polluted with a "cache" folder
				forceClean = false,
				javaOnly = false,
				compileOrder = CompileOrder.JavaThenScala,
				incOptions = IncOptions.Default,
				outputRelations = None,
				outputProducts = None,
				mirrorAnalysis = false)
		}

		def setupZincCompiler(): Setup = {
			def jarMatching(regex: String): JFile = {
				val compiledRegex = regex.r
				val jarUrl = classpathURLs
					.filter(url => compiledRegex.findFirstMatchIn(url.toString).isDefined)
					.headOption
					.getOrElse(throw new RuntimeException("Can't find the jar matching " + regex))

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
				javaHomeDir = None)
		}

		// Setup the compiler
		val setup = setupZincCompiler
		val zincLogger = new Logger {
			def error(arg: xsbti.F0[String]) { logger.error(arg.apply) }
			def warn(arg: xsbti.F0[String]) { logger.warn(arg.apply) }
			def info(arg: xsbti.F0[String]) { logger.info(arg.apply) }
			def debug(arg: xsbti.F0[String]) { logger.debug(arg.apply) }
			def trace(arg: xsbti.F0[Throwable]) { logger.trace("", arg.apply) }
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
