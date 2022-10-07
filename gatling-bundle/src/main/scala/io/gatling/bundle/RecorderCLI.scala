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

package io.gatling.bundle

import io.gatling.bundle.commands.RecorderCommand
import io.gatling.core.cli.GatlingOptionParser
import io.gatling.recorder.cli.CommandLineConstants._

object RecorderCLI {
  def main(args: Array[String]): Unit = {
    val parser = new GatlingOptionParser[Unit]("recorder") {
      help(Help)

      opt[Int](LocalPort)
      opt[String](ProxyHost)
      opt[Int](ProxyPort)
      opt[Int](ProxyPortSsl)
      opt[String](SimulationsFolder)
      opt[String](ResourcesFolder)
      opt[String](ClassName)
      opt[String](Package)
      opt[String](Encoding)
      opt[Boolean](FollowRedirect)
      opt[Boolean](AutomaticReferer)
      opt[Boolean](InferHtmlResources)
      opt[String](Mode)
      opt[Boolean](Headless)
      opt[String](HarFilePath)
    }

    if (parser.parse(args)) {
      new RecorderCommand(args.toList).run()
    }
  }
}
