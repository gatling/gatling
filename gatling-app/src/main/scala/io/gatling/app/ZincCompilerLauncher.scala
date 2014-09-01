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

import java.net.URI

import scala.sys.process.Process
import scala.util.Properties.{ javaClassPath, jdkHome, isWin }

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles.{ binariesDirectory, GatlingHome }
import io.gatling.core.util.StringHelper.RichString
import io.gatling.core.util.UriHelper._

object ZincCompilerLauncher {

  def apply(sourceDirectory: URI): URI = {

    val binDirectory = binariesDirectory.getOrElse(GatlingHome / "target")
    val javaHome = jdkHome.trimToOption.getOrElse(throw new IllegalStateException("Couldn't locate java, try setting JAVA_HOME environment variable."))
    val javaExe = pathToUri(javaHome) / "bin" / (if (isWin) "java.exe" else "java")
    val classesDirectory = binDirectory / "classes"
    classesDirectory.toFile.mkdirs()

    val classPath = Seq("-cp", javaClassPath)
    val jvmArgs = configuration.core.zinc.jvmArgs.toSeq
    val clazz = Seq("io.gatling.app.ZincCompiler")
    val args = Seq(GatlingHome, sourceDirectory.toFile, binDirectory.toFile, classesDirectory.toFile, configuration.core.encoding).map(_.toString)

    val process = Process(javaExe.toFile.getAbsolutePath, Seq(classPath, jvmArgs, clazz, args).flatten)

    if (process.! != 0) {
      println("Compilation failed")
      System.exit(1)
    }

    classesDirectory
  }
}
