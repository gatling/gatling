/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.cli

import java.nio.file.Path

import scala.collection.mutable

import io.gatling.core.cli.CliOption
import io.gatling.recorder.render.template.RenderingFormat

final class RecorderOptionsBuilder {
  private val options = mutable.Map.empty[CliOption, Option[String]]

  def simulationsFolder(folder: Path): RecorderOptionsBuilder = {
    options += RecorderOptions.SimulationsFolder -> Some(folder.toString)
    this
  }

  def simulationPackage(pkg: String): RecorderOptionsBuilder = {
    options += RecorderOptions.Package -> Some(pkg)
    this
  }

  def resourcesFolder(folder: Path): RecorderOptionsBuilder = {
    options += RecorderOptions.ResourcesFolder -> Some(folder.toString)
    this
  }

  def simulationClassName(name: String): RecorderOptionsBuilder = {
    options += RecorderOptions.ClassName -> Some(name)
    this
  }

  def simulationFormatJava: RecorderOptionsBuilder =
    simulationFormat(RenderingFormat.defaultFromJvm)

  def simulationFormatJava11: RecorderOptionsBuilder =
    simulationFormat(RenderingFormat.Java11)

  def simulationFormatJava17: RecorderOptionsBuilder =
    simulationFormat(RenderingFormat.Java17)

  def simulationFormatKotlin: RecorderOptionsBuilder =
    simulationFormat(RenderingFormat.Kotlin)

  def simulationFormatScala: RecorderOptionsBuilder =
    simulationFormat(RenderingFormat.Scala)

  private[recorder] def simulationFormat(format: RenderingFormat): RecorderOptionsBuilder = {
    options += RecorderOptions.Format -> Some(format.configValue)
    this
  }

  def build: Array[String] = options.toList.flatMap { case (constant, maybeValue) =>
    val paramName = s"-${constant.abbr}"
    maybeValue match {
      case Some(value) => List(paramName, value)
      case _           => paramName :: Nil
    }
  }.toArray
}
