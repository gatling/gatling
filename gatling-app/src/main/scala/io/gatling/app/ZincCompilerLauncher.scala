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

import scala.tools.nsc.io.Directory
import scala.tools.nsc.io.Path.string2path

import org.apache.commons.exec.{ CommandLine, DefaultExecutor, PumpStreamHandler, ShutdownHookProcessDestroyer }

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles

object ZincCompilerLauncher extends Logging {

	def apply(sourceDirectory: Directory): Directory = {

		val binDirectory = GatlingFiles.binariesDirectory.getOrElse(GatlingFiles.GATLING_HOME / "target")
		val javaHome = Option(System.getProperty("java.home")).orElse(Option(System.getenv("JAVA_HOME"))).getOrElse(throw new IllegalStateException("Couldn't locate java, try setting JAVA_HOME environment variable."))
		val javaExe = javaHome / "bin" / (if (System.getProperty("os.name").contains("dos")) "java.exe" else "java")
		val javaClasspath = System.getProperty("java.class.path")
		val classesDirectory = Directory(binDirectory / "classes")

		val cl = new CommandLine(javaExe.toString)
		cl.addArgument("-cp")
		cl.addArgument(javaClasspath)
		cl.addArgument("-Xmn200M")
		cl.addArgument("-Xss2M")
		cl.addArgument("io.gatling.app.ZincCompiler")
		cl.addArgument(GatlingFiles.GATLING_HOME.toString)
		cl.addArgument(sourceDirectory.toString)
		cl.addArgument(binDirectory.toString)
		cl.addArgument(classesDirectory.toString)
		cl.addArgument(configuration.core.encoding)

		logger.debug(s"Launching Zinc: $cl")

		val exec = new DefaultExecutor
		exec.setStreamHandler(new PumpStreamHandler(System.out, System.err, System.in))
		exec.setProcessDestroyer(new ShutdownHookProcessDestroyer)
		if (exec.execute(cl) != 0) throw new RuntimeException("Compiling failed")

		classesDirectory
	}
}
