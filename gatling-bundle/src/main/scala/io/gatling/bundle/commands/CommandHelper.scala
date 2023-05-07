/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.bundle.commands

import java.nio.file.{ Files, Path, Paths }
import java.util.ResourceBundle

import scala.jdk.StreamConverters._

private[bundle] object CommandHelper {
  val GatlingVersion: String = ResourceBundle.getBundle("gatling-version").getString("version")

  val GatlingHome: Path = optionEnv("GATLING_HOME")
    .map(Paths.get(_))
    .getOrElse {
      try {
        Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI).getParent.getParent
      } catch {
        case _: NullPointerException =>
          throw new IllegalStateException("""
                                            |'GATLING_HOME' environment variable is not set and Gatling couldn't infer it either.
                                            |Please set the 'GATLING_HOME' environment variable to the root of the Gatling bundle.
                                            |""".stripMargin)
      }
    }
    .toAbsolutePath
  println(s"GATLING_HOME is set to $GatlingHome")

  val JavaOptsEnvVar: List[String] = optionListEnv("JAVA_OPTS")

  val UserLibDirectory: Path = GatlingHome.resolve("user-files").resolve("lib")
  val DefaultResourcesDirectory: Path = GatlingHome.resolve("user-files").resolve("resources")
  val TargetDirectory: Path = GatlingHome.resolve("target")
  val DefaultBinariesDirectory: Path = TargetDirectory.resolve("test-classes")
  val ConfDirectory: Path = optionEnv("GATLING_CONF").map(Paths.get(_).toAbsolutePath).getOrElse(GatlingHome.resolve("conf"))

  val GatlingLibs: List[String] = {
    val libDirectory: Path = GatlingHome.resolve("lib")
    listFiles(libDirectory)
  }

  val UserLibs: List[String] = listFiles(UserLibDirectory)
  val GatlingConfFiles: List[String] = ConfDirectory.toString +: listFiles(ConfDirectory)

  private def optionEnv(env: String): Option[String] =
    sys.env.get(env).map(_.trim).filter(_.nonEmpty)

  def optionListEnv(env: String): List[String] =
    optionEnv(env).map(_.split(" ").toList).getOrElse(Nil)

  private def listFiles(directory: Path): List[String] = {
    require(Files.isDirectory(directory), s"Gatling bundle directory $directory is missing")
    Files
      .list(directory)
      .toScala(List)
      .map(_.toAbsolutePath.toString)
  }
}
