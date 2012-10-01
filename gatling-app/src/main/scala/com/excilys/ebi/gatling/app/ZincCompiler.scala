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

import java.io.{File => JFile}

import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.{Path, Directory}
import scala.tools.nsc.util.ClassPath

import com.excilys.ebi.gatling.core.config.GatlingFiles
import com.typesafe.zinc.{Setup, Inputs, Compiler}

import grizzled.slf4j.Logging
import xsbti.api.Compilation
import xsbti.compile.CompileOrder
import xsbti.Logger

object ZincCompiler extends Logging {

	def compilerInterfaceJarLocation() = {
		val classpathUrls = java.lang.Thread.currentThread.getContextClassLoader match {
			case cl: java.net.URLClassLoader => cl.getURLs.toSeq
			case _ => throw new RuntimeException("classloader is not a URLClassLoader")
		}

		val compilerInterfaceRegex = """(.*compiler-interface-\d+.\d+.\d+-sources.jar)$""".r
		val filteredClasspathUrls = classpathUrls.filter(url => url.toString match {
			case compilerInterfaceRegex(_) => true
			case _ => false
		})
		val compilerInterfaceURL = filteredClasspathUrls.firstOption.getOrElse(throw new RuntimeException("Can't find the compiler-interface jar"))

		new JFile(compilerInterfaceURL.getPath)
	}

	def setupZincCompiler(): Setup = {
		val scalaCompiler = ClassPath.scalaCompiler.getOrElse(throw new RuntimeException("No Scala compiler available"))
		val scalaLibrary = ClassPath.scalaLibrary.getOrElse(throw new RuntimeException("No Scala library available"))
		val scalaExtra: Seq[JFile] = Nil
		// TODO 
		val sbtInterface: JFile = new JFile(classOf[Compilation].getProtectionDomain.getCodeSource.getLocation.getPath)
		// TODO
		val compilerInterfaceSrc: JFile = compilerInterfaceJarLocation()
		val javaHomeDir: Option[JFile] = None
		
		Setup.setup(scalaCompiler.jfile, scalaLibrary.jfile, scalaExtra, sbtInterface, compilerInterfaceSrc, javaHomeDir)
	}

	def simulationInputs(sourceDirectory: Directory, classesDir: Path, zincCacheFile: Path) = {
		val classPathUrls = java.lang.Thread.currentThread.getContextClassLoader match {
			case cl: java.net.URLClassLoader => cl.getURLs.toSeq
			case _ => throw new RuntimeException("classloader is not a URLClassLoader")
		}
		val classpath: Seq[JFile] = classPathUrls.map(url => new JFile(url.getPath))

		val scalaSourceFiles = sourceDirectory.deepFiles.filter(_.hasExtension("scala")).toList
		val sources: Seq[JFile] = scalaSourceFiles.map(f => f.jfile)

		val classesDirectory: JFile = classesDir jfile
		val scalacOptions: Seq[String] = Nil
		val javacOptions: Seq[String] = Nil
		val analysisCache: Option[JFile] = Some(zincCacheFile jfile)
		val analysisCacheMap: Map[JFile, JFile] = Map[JFile, JFile]()
		val javaOnly: Boolean = false
		val compileOrder: CompileOrder = CompileOrder.JavaThenScala
		val outputRelations: Option[JFile] = None
		val outputProducts: Option[JFile] = None

		Inputs.inputs(classpath, sources, classesDirectory, scalacOptions, javacOptions,
			analysisCache, analysisCacheMap, javaOnly, compileOrder, outputRelations, outputProducts)
	}

	def apply(sourceDirectory: Directory) = {

		// Setup the compiler
		val setup = setupZincCompiler
		val zincLogger = new ZincLogger
		val zincCompiler = Compiler.create(setup, zincLogger)

		val binDir = GatlingFiles.binariesDirectory.getOrElse(
			GatlingFiles.GATLING_HOME / "bin")
		val classesDir = binDir / "classes"
		val zincCacheFile = binDir / "zincCache"

		// Define the inputs
		val inputs = simulationInputs(sourceDirectory, classesDir, zincCacheFile)
		Inputs.debug(inputs, zincLogger)

		zincCompiler.compile(inputs)(zincLogger)

		classesDir
	}

	class ZincLogger extends Logger {
		// TODO : define a proper strategy in logback.xml
		def error(arg: xsbti.F0[String]) = {
			println("->ERROR: " + arg.apply())
			logger.error(arg.apply)
		}
		def warn(arg: xsbti.F0[String]) = {
			println("->WARN: " + arg.apply())
			logger.warn(arg.apply)
		}
		def info(arg: xsbti.F0[String]) = {
			println("->INFO: " + arg.apply())
			logger.info(arg.apply)
		}
		def debug(arg: xsbti.F0[String]) = {
			println("->DEBUG: " + arg.apply())
			logger.debug(arg.apply)
		}
		def trace(arg: xsbti.F0[Throwable]) = {
			println("->TRACE: " + arg.apply())
			logger.trace(arg.apply)
		}
	}
}
