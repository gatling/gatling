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
import scala.tools.nsc.util.ClassPath

import com.excilys.ebi.gatling.core.config.GatlingFiles
import com.excilys.ebi.gatling.core.config.GatlingFiles.GATLING_HOME
import com.typesafe.zinc.{ Compiler, Inputs, Setup }

import grizzled.slf4j.Logging
import xsbti.Logger
import xsbti.api.Compilation
import xsbti.compile.CompileOrder

object ZincCompiler extends Logging {

	val classpathURLs = Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader].getURLs

	def setupZincCompiler(): Setup = {
		def jarMatching(regex: String): JFile = {
			val compiledRegex = regex.r
			val jarUrl = classpathURLs
				.filter(url => compiledRegex.findFirstMatchIn(url.toString).isDefined)
				.headOption
				.getOrElse(throw new RuntimeException("Can't find the jar matching " + regex))

			new JFile(jarUrl.toURI)
		}

		val scalaCompiler = ClassPath.scalaCompiler.getOrElse(throw new RuntimeException("No Scala compiler available")).jfile
		val scalaLibrary = ClassPath.scalaLibrary.getOrElse(throw new RuntimeException("No Scala library available")).jfile
		val sbtInterfaceSrc: JFile = new JFile(classOf[Compilation].getProtectionDomain.getCodeSource.getLocation.toURI)
		val compilerInterfaceSrc: JFile = jarMatching("""(.*compiler-interface-\d+.\d+.\d+-sources.jar)$""")

		Setup.setup(scalaCompiler = scalaCompiler,
			scalaLibrary = scalaLibrary,
			scalaExtra = Nil,
			sbtInterface = sbtInterfaceSrc,
			compilerInterfaceSrc = compilerInterfaceSrc,
			javaHomeDir = None)
	}

	def simulationInputs(sourceDirectory: Directory, binDir: Path) = {
		val classpath = classpathURLs.map(url => new JFile(url.toURI))

		val sources = sourceDirectory
			.deepFiles
			.toList
			.collect { case file if (file.hasExtension("scala")) => file.jfile }

		// avoids having GATLING_HOME polluted with a "cache" folder
		val analysisCacheMap: Map[JFile, JFile] = Map[JFile, JFile](
			(GATLING_HOME / "bin").jfile -> (binDir / "cache" / "bin").jfile,
			(GATLING_HOME / "conf").jfile -> (binDir / "cache" / "conf").jfile,
			(GATLING_HOME / "user-files").jfile -> (binDir / "cache" / "user-files").jfile)

		Inputs.inputs(classpath = classpath,
			sources = sources,
			classesDirectory = (binDir / "classes").jfile,
			scalacOptions = Seq("-deprecation"),
			javacOptions = Nil,
			analysisCache = Some((binDir / "zincCache").jfile),
			analysisCacheMap = analysisCacheMap,
			forceClean = false,
			javaOnly = false,
			compileOrder = CompileOrder.JavaThenScala,
			outputRelations = None,
			outputProducts = None,
			mirrorAnalysis = false)
	}

	def apply(sourceDirectory: Directory): Directory = {

		// Setup the compiler
		val setup = setupZincCompiler
		val zincLogger = new ZincLogger
		val zincCompiler = Compiler.create(setup, zincLogger)

		val binDir = GatlingFiles.binariesDirectory.getOrElse(GATLING_HOME / "target")

		// Define the inputs
		val inputs = simulationInputs(sourceDirectory, binDir)
		Inputs.debug(inputs, zincLogger)

		zincCompiler.compile(inputs)(zincLogger)

		Directory(inputs.classesDirectory)
	}

	class ZincLogger extends Logger {
		def error(arg: xsbti.F0[String]) {
			logger.error(arg.apply)
		}
		def warn(arg: xsbti.F0[String]) {
			logger.warn(arg.apply)
		}
		def info(arg: xsbti.F0[String]) {
			logger.info(arg.apply)
		}
		def debug(arg: xsbti.F0[String]) {
			logger.debug(arg.apply)
		}
		def trace(arg: xsbti.F0[Throwable]) {
			logger.trace(arg.apply)
		}
	}
}
