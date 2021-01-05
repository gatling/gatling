/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder

import java.nio.file.Path

import io.gatling.commons.util.DefaultClock
import io.gatling.recorder.cli.ArgsParser
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.controller.RecorderController

object GatlingRecorder {

  def main(args: Array[String]): Unit = fromArgs(args)

  def fromArgs(args: Array[String]): Unit = {
    val argsParser = new ArgsParser(args)
    argsParser.parseArguments.map(overrides => initRecorder(overrides, None))
  }

  def fromMap(props: ConfigOverrides, recorderConfigFile: Option[Path]): RecorderController =
    initRecorder(props, recorderConfigFile)

  private def initRecorder(props: ConfigOverrides, recorderConfigFile: Option[Path]) = {
    RecorderConfiguration.initialSetup(props, recorderConfigFile)
    new RecorderController(new DefaultClock)
  }
}
