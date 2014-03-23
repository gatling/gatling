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
package io.gatling.recorder

import io.gatling.recorder.CommandLineConstants._
import io.gatling.recorder.config.RecorderPropertiesBuilder
import io.gatling.recorder.controller.RecorderController
import scopt.OptionParser

object GatlingRecorder {

  private val props = new RecorderPropertiesBuilder

  private val cliOptsParser = new OptionParser[Unit]("gatling-recorder") {
    help(HELP).abbr(HELP_SHORT).text("Show help (this message) and exit")
    opt[Int](LOCAL_PORT).abbr(LOCAL_PORT_SHORT).valueName("<port>").foreach(props.localPort).text("Local port used by Gatling Proxy for HTTP")
    opt[Int](LOCAL_PORT_SSL).abbr(LOCAL_PORT_SSL_SHORT).valueName("<port>").foreach(props.localSslPort).text("Local port used by Gatling Proxy for HTTPS")
    opt[String](PROXY_HOST).abbr(PROXY_HOST_SHORT).valueName("<host>").foreach(props.proxyHost).text("Outgoing proxy host")
    opt[Int](PROXY_PORT).abbr(PROXY_PORT_SHORT).valueName("<port>").foreach(props.proxyPort).text("Outgoing proxy port for HTTP")
    opt[Int](PROXY_PORT_SSL).abbr(PROXY_PORT_SSL_SHORT).valueName("<port>").foreach(props.proxySslPort).text("Outgoing proxy port for HTTPS")
    opt[String](OUTPUT_FOLDER).abbr(OUTPUT_FOLDER_SHORT).valueName("<folderName>").foreach(props.simulationOutputFolder).text("Uses <folderName> as the folder where generated simulations will be stored")
    opt[String](REQUEST_BODIES_FOLDER).abbr(REQUEST_BODIES_FOLDER_SHORT).valueName("<folderName>").foreach(props.requestBodiesFolder).text("Uses <folderName> as the folder where request bodies are stored")
    opt[String](CLASS_NAME).abbr(CLASS_NAME_SHORT).foreach(props.simulationClassName).text("Sets the name of the generated class")
    opt[String](PACKAGE).abbr(PACKAGE_SHORT).foreach(props.simulationPackage).text("Sets the package of the generated class")
    opt[String](ENCODING).abbr(ENCODING_SHORT).foreach(props.encoding).text("Sets the encoding used in the recorder")
    opt[Boolean](FOLLOW_REDIRECT).abbr(FOLLOW_REDIRECT_SHORT).foreach(props.followRedirect).text("Sets the follow redirect option to true")
  }

  def main(args: Array[String]) {
    if (cliOptsParser.parse(args))
      RecorderController(props.build)
  }
}