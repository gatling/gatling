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

import io.gatling.core.cli.GatlingOptionParser
import io.gatling.recorder.ConfigOverrides
import io.gatling.recorder.cli.CommandLineConstants._
import io.gatling.recorder.config.{ RecorderMode, RecorderPropertiesBuilder }

private[recorder] class ArgsParser(args: Array[String]) {
  private val props = new RecorderPropertiesBuilder

  private val cliOptsParser = new GatlingOptionParser[Unit]("recorder") {
    opt[Int](LocalPort)
      .foreach(props.localPort)

    opt[String](ProxyHost)
      .foreach(props.proxyHost)

    opt[Int](ProxyPort)
      .foreach(props.proxyPort)

    opt[Int](ProxyPortSsl)
      .foreach(props.proxySslPort)

    opt[String](SimulationsFolder)
      .foreach(props.simulationsFolder)

    opt[String](ResourcesFolder)
      .foreach(props.resourcesFolder)

    opt[String](ClassName)
      .foreach(props.simulationClassName)

    opt[String](Package)
      .foreach(props.simulationPackage)

    opt[String](Encoding)
      .foreach(props.encoding)

    opt[Boolean](FollowRedirect)
      .foreach(props.followRedirect)

    opt[Boolean](AutomaticReferer)
      .foreach(props.automaticReferer)

    opt[Boolean](InferHtmlResources)
      .foreach(props.inferHtmlResources)

    opt[String](Mode)
      .foreach(m => props.mode(RecorderMode(m)))

    opt[Boolean](Headless)
      .foreach(props.headless)

    opt[String](HarFilePath)
      .foreach(props.harFilePath)
  }

  def parseArguments: Option[ConfigOverrides] =
    if (cliOptsParser.parse(args)) Some(props.build) else None
}
