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
import java.nio.file.{ Files, Paths }
import java.util.ResourceBundle

import scala.jdk.CollectionConverters._
import scala.util.control.NoStackTrace

import io.gatling.bundle.BundleIO
import io.gatling.plugin.util.Fork

private[commands] object CommandHelper {
  private class InvalidGatlingHomeException
      extends IllegalStateException("""
                                      |Gatling Home is not set correctly,
                                      |Please set the 'GATLING_HOME' environment variable to the root of the Gatling bundle.
                                      |""".stripMargin)
      with NoStackTrace

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
      case _: NullPointerException => throw new IllegalStateException("""
                                                                        |Couldn't dynamically compute the Gatling Home.
                                                                        |Please set the 'GATLING_HOME' environment variable to the root of the Gatling bundle.
                                                                        |""".stripMargin)
    }
  }
  def gatlingConf: File = optionEnv("GATLING_CONF").map(new File(_)).getOrElse(new File(gatlingHome, "conf"))

  def systemJavaOpts: List[String] = optionListEnv("JAVA_OPTS")
  def GATLING_JVM_ARGS: List[String] = List(
    "-server",
    "-Xmx1G",
    "-XX:+UseG1GC",
    "-XX:+ParallelRefProcEnabled",
    "-XX:+HeapDumpOnOutOfMemoryError",
    "-XX:MaxInlineLevel=20",
    "-XX:MaxTrivialSize=12",
    "-XX:-UseBiasedLocking"
  )

  if (Files.notExists(Paths.get(gatlingHome))) {
    throw new InvalidGatlingHomeException
  }

  def gatlingLibs: List[String] = listFiles(new File(gatlingHome, "lib"))
  def gatlingConfFiles: List[String] = listFiles(gatlingConf) ++ List(gatlingConf.getAbsolutePath)
  def userResources: List[String] = List(s"$gatlingHome${File.separator}user-files${File.separator}resources")

  def listFiles(file: File): List[String] = {
    if (!file.exists() || !file.isDirectory) {
      throw new InvalidGatlingHomeException
    }
    file
      .listFiles()
      .toList
      .map(_.getAbsolutePath)
  }

  def compile(args: List[String]): Unit = {

    println(s"GATLING_HOME is set to $gatlingHome")

    val compilerOpts = List("-Xss100M") ++ GATLING_JVM_ARGS ++ systemJavaOpts

    val classPath = gatlingLibs ++ gatlingConfFiles

    val extraCompilerOptions = optionListEnv("EXTRA_SCALAC_OPTIONS")
      .flatMap(options => List("-eso", options))

    new Fork(
      "io.gatling.compiler.ZincCompiler",
      classPath.asJava,
      compilerOpts.asJava,
      (extraCompilerOptions ++ args).asJava,
      java,
      true,
      BundleIO.getLogger,
      new File(gatlingHome)
    ).run()
  }
}
