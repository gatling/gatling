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

package io.gatling.app.cli

import java.nio.file.Path

import scala.collection.mutable

import io.gatling.core.cli.CliOption

final class GatlingOptionsBuilder {
  private val options = mutable.Map.empty[CliOption, Option[String]]

  def noReports(): GatlingOptionsBuilder = {
    options += GatlingOptions.NoReports -> None
    this
  }

  def reportsOnly(v: String): GatlingOptionsBuilder = {
    options += GatlingOptions.ReportsOnly -> Some(v)
    this
  }

  def resultsDirectory(v: Path): GatlingOptionsBuilder = {
    options += GatlingOptions.ResultsFolder -> Some(v.toString)
    this
  }

  def simulationClass(v: String): GatlingOptionsBuilder = {
    options += GatlingOptions.Simulation -> Some(v)
    this
  }

  def runDescription(v: String): GatlingOptionsBuilder = {
    options += GatlingOptions.RunDescription -> Some(v)
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
