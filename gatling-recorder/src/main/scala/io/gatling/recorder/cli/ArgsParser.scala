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
package io.gatling.recorder.cli

import scopt.{ OptionDef, OptionParser, Read }

import io.gatling.recorder.ConfigOverrides
import io.gatling.recorder.cli.CommandLineConstants._
import io.gatling.recorder.config.RecorderPropertiesBuilder

private[recorder] class ArgsParser(args: Array[String]) {

  private class RecorderOptionParser extends OptionParser[Unit]("recorder") {
    def help(constant: CommandLineConstant): OptionDef[Unit, Unit] =
      help(constant.full).abbr(constant.abbr)

    def opt[A: Read](constant: CommandLineConstant): OptionDef[A, Unit] =
      opt[A](constant.full).abbr(constant.abbr)
  }

  private val props = new RecorderPropertiesBuilder

  private val cliOptsParser = new RecorderOptionParser {

    help(Help).text("Show help (this message) and exit")

    opt[Int](LocalPort)
      .foreach(props.localPort)
      .valueName("<port>")
      .text("Local port used by Gatling Proxy for HTTP/HTTPS")

    opt[String](ProxyHost)
      .foreach(props.proxyHost)
      .valueName("<host>")
      .text("Outgoing proxy host")

    opt[Int](ProxyPort)
      .foreach(props.proxyPort)
      .valueName("<port>")
      .text("Outgoing proxy port for HTTP")

    opt[Int](ProxyPortSsl)
      .foreach(props.proxySslPort)
      .valueName("<port>")
      .text("Outgoing proxy port for HTTPS")

    opt[String](OutputFolder)
      .foreach(props.simulationOutputFolder)
      .valueName("<folderName>")
      .text("Uses <folderName> as the folder where generated simulations will be stored")

    opt[String](BodiesFolder)
      .foreach(props.bodiesFolder)
      .valueName("<folderName>")
      .text("Uses <folderName> as the folder where bodies are stored")

    opt[String](ClassName)
      .foreach(props.simulationClassName)
      .valueName("<className>")
      .text("Sets the name of the generated class")

    opt[String](Package)
      .foreach(props.simulationPackage)
      .valueName("<package>")
      .text("Sets the package of the generated class")

    opt[String](Encoding)
      .foreach(props.encoding)
      .valueName("<encoding>")
      .text("Sets the encoding used in the recorder")

    opt[Boolean](FollowRedirect)
      .foreach(props.followRedirect)
      .text("""Sets the "Follow Redirects" option to true""")

    opt[Boolean](AutomaticReferer)
      .foreach(props.automaticReferer)
      .text("""Sets the "Automatic Referers" option to true""")

    opt[Boolean](InferHtmlResources)
      .foreach(props.inferHtmlResources)
      .text("""Sets the "Fetch html resources" option to true""")
  }

  def parseArguments: Option[ConfigOverrides] =
    if (cliOptsParser.parse(args)) Some(props.build) else None
}
