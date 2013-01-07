/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.app

import java.io.{ File => JFile }
import java.net.URLClassLoader

import scala.tools.nsc.io.{ Directory, Path }
import scala.tools.nsc.io.Path.{ jfile2path, string2path }

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles
import com.excilys.ebi.gatling.core.config.GatlingFiles.GATLING_HOME
import com.typesafe.zinc.{ Compiler, Inputs, Setup }

import grizzled.slf4j.Logging
import xsbti.Logger
import xsbti.api.Compilation
import xsbti.compile.CompileOrder

object ZincCompiler extends Logging {

	def apply(sourceDirectory: Directory): Directory = {

		val classpathURLs = Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader].getURLs

		def simulationInputs(sourceDirectory: Directory, binDir: Path) = {
			val classpath = classpathURLs.map(url => new JFile(url.toURI))

			val sources = sourceDirectory
				.deepFiles
				.collect { case file if (file.hasExtension("scala")) => file.jfile }
				.toList

			def analysisCacheMapEntry(directoryName: String) = (GATLING_HOME / directoryName).jfile -> (binDir / "cache" / directoryName).jfile

			Inputs.inputs(classpath = classpath,
				sources = sources,
				classesDirectory = (binDir / "classes").jfile,
				scalacOptions = Seq("-encoding", configuration.simulation.encoding, "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-language:implicitConversions", "-language:reflectiveCalls", "-language:postfixOps"),
				javacOptions = Nil,
				analysisCache = Some((binDir / "zincCache").jfile),
				analysisCacheMap = Map(analysisCacheMapEntry("bin"), analysisCacheMapEntry("conf"), analysisCacheMapEntry("user-files")), // avoids having GATLING_HOME polluted with a "cache" folder
				forceClean = false,
				javaOnly = false,
				compileOrder = CompileOrder.JavaThenScala,
				outputRelations = None,
				outputProducts = None)
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

			val scalaCompiler = jarMatching("""(.*scala-compiler-.*\.jar)$""")
			val scalaLibrary = jarMatching("""(.*scala-library-.*\.jar)$""")
			val scalaReflect = jarMatching("""(.*scala-reflect-.*\.jar)$""")
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
			def trace(arg: xsbti.F0[Throwable]) { logger.trace(arg.apply) }
		}

		val zincCompiler = Compiler.create(setup, zincLogger)

		val binDir = GatlingFiles.binariesDirectory.getOrElse(GATLING_HOME / "target")

		// Define the inputs
		val inputs = simulationInputs(sourceDirectory, binDir)
		Inputs.debug(inputs, zincLogger)

		zincCompiler.compile(inputs)(zincLogger)

		Directory(inputs.classesDirectory)
	}
}
