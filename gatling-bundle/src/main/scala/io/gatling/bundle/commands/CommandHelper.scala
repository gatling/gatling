/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import java.io.File
import java.nio.file.{ Files, Path, Paths }
import java.util.ResourceBundle

import scala.jdk.CollectionConverters._
import scala.jdk.StreamConverters._

import io.gatling.bundle.{ BundleIO, CommandArguments }
import io.gatling.plugin.GatlingConstants
import io.gatling.plugin.util.Fork

private[commands] object CommandHelper {

  def gatlingVersion: String = ResourceBundle.getBundle("gatling-version").getString("version")

  def optionEnv(env: String): Option[String] =
    sys.env.get(env).map(_.trim).filter(_.nonEmpty)

  def optionListEnv(env: String): List[String] =
    optionEnv(env).map(_.split(" ").toList).getOrElse(Nil)

  def java: String = optionEnv("JAVA_HOME") match {
    case Some(java) => s"$java${File.separator}bin${File.separator}java"
    case _          => "java"
  }

  def gatlingHome: String = optionEnv("GATLING_HOME").getOrElse {
    try {
      Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI).getParent.getParent.toAbsolutePath.toString
    } catch {
      case _: NullPointerException =>
        throw new IllegalStateException("""
                                          |'GATLING_HOME' environment variable is not set and Gatling couldn't infer it either.
                                          |Please set the 'GATLING_HOME' environment variable to the root of the Gatling bundle.
                                          |""".stripMargin)
    }
  }

  def systemJavaOpts: List[String] = optionListEnv("JAVA_OPTS")

  val gatlingConfDirectory: Path = optionEnv("GATLING_CONF").map(Paths.get(_)).getOrElse(Paths.get(gatlingHome, "conf")).toAbsolutePath
  val gatlingLibsDirectory: Path = Paths.get(gatlingHome, "lib").toAbsolutePath
  val userLibsDirectory: Path = Paths.get(gatlingHome, "user-files", "lib").toAbsolutePath
  val userResourcesDirectory: Path = Paths.get(gatlingHome, "user-files", "resources").toAbsolutePath
  val targetDirectory: Path = Paths.get(gatlingHome, "target").toAbsolutePath
  val targetTestClassesDirectory: Path = Paths.get(gatlingHome, "target", "test-classes").toAbsolutePath

  def gatlingLibs: List[String] = listFiles(gatlingLibsDirectory)
  def userLibs: List[String] = listFiles(userLibsDirectory)
  def gatlingConfFiles: List[String] = listFiles(gatlingConfDirectory) ++ List(gatlingConfDirectory.toString)
  def userResources: List[String] = List(userResourcesDirectory.toString)

  def listFiles(directory: Path): List[String] = {
    require(Files.isDirectory(directory), s"Gatling bundle directory $directory is missing")
    Files
      .list(directory)
      .toScala(List)
      .map(_.toAbsolutePath.toString)
  }

  def compile(config: CommandArguments, args: List[String], maxJavaVersion: Option[Int]): Unit = {

    println(s"GATLING_HOME is set to $gatlingHome")

    val compilerMemoryOptions = List("-Xmx1G", "-Xss100M")
    // Note: options which come later in the list can override earlier ones (because the java command will use the last
    // occurrence in its arguments list in case of conflict)
    val compilerJavaOptions = GatlingConstants.DEFAULT_JVM_OPTIONS_BASE.asScala ++ compilerMemoryOptions ++ systemJavaOpts ++ config.extraJavaOptionsCompile

    val classPath = gatlingLibs ++ userLibs ++ gatlingConfFiles

    val extraJavacOptions = maxJavaVersion match {
      case Some(maxVersion) if GatlingConstants.JAVA_MAJOR_VERSION > maxVersion =>
        println(
          s"Currently running on unsupported Java version ${GatlingConstants.JAVA_MAJOR_VERSION}; Java code will be compiled with the '--release $maxVersion' option"
        )
        List("-ejo", s"--release,$maxVersion")
      case _ =>
        Nil
    }

    val extraScalacOptions = optionListEnv("EXTRA_SCALAC_OPTIONS")
      .flatMap(options => List("-eso", options))

    new Fork(
      "io.gatling.compiler.ZincCompiler",
      classPath.asJava,
      compilerJavaOptions.asJava,
      (extraJavacOptions ++ extraScalacOptions ++ args).asJava,
      java,
      true,
      BundleIO.getLogger,
      new File(gatlingHome)
    ).run()
  }
}
