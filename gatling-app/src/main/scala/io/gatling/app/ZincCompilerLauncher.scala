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

import scala.sys.process.Process
import scala.tools.nsc.io.Directory
import scala.tools.nsc.io.Path.string2path
import scala.util.Properties.{ javaClassPath, jdkHome, isWin }

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles.{ binariesDirectory, GATLING_HOME }
import io.gatling.core.util.StringHelper.RichString

object ZincCompilerLauncher {

	def apply(sourceDirectory: Directory): Directory = {

		val binDirectory = binariesDirectory.getOrElse(GATLING_HOME / "target")
		val javaHome = jdkHome.trimToOption.getOrElse(throw new IllegalStateException("Couldn't locate java, try setting JAVA_HOME environment variable."))
		val javaExe = javaHome / "bin" / (if (isWin) "java.exe" else "java")
		val classesDirectory = Directory(binDirectory / "classes")

		val classPath = Seq("-cp", javaClassPath)
		val jvmArgs = configuration.core.zinc.jvmArgs.toSeq
		val clazz = Seq("io.gatling.app.ZincCompiler")
		val args = Seq(GATLING_HOME, sourceDirectory, binDirectory, classesDirectory, configuration.core.encoding).map(_.toString)

		val process = Process(javaExe.toString(), Seq(classPath, jvmArgs, clazz, args).flatten)

		if (process.! != 0) {
			println("Compilation failed")
			System.exit(1)
		}

		classesDirectory
	}
}
