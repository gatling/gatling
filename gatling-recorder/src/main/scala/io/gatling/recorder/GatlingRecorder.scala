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
    help(Help.full).abbr(Help.abbr).text("Show help (this message) and exit")
    opt[Int](LocalPort.full).abbr(LocalPort.abbr).valueName("<port>").foreach(props.localPort).text("Local port used by Gatling Proxy for HTTP")
    opt[Int](LocalPortSsl.full).abbr(LocalPortSsl.abbr).valueName("<port>").foreach(props.localSslPort).text("Local port used by Gatling Proxy for HTTPS")
    opt[String](ProxyHost.full).abbr(ProxyHost.abbr).valueName("<host>").foreach(props.proxyHost).text("Outgoing proxy host")
    opt[Int](ProxyPort.full).abbr(ProxyPort.abbr).valueName("<port>").foreach(props.proxyPort).text("Outgoing proxy port for HTTP")
    opt[Int](ProxyPortSsl.full).abbr(ProxyPortSsl.abbr).valueName("<port>").foreach(props.proxySslPort).text("Outgoing proxy port for HTTPS")
    opt[String](OutputFolder.full).abbr(OutputFolder.abbr).valueName("<folderName>").foreach(props.simulationOutputFolder).text("Uses <folderName> as the folder where generated simulations will be stored")
    opt[String](RequestBodiesFolder.full).abbr(RequestBodiesFolder.abbr).valueName("<folderName>").foreach(props.requestBodiesFolder).text("Uses <folderName> as the folder where request bodies are stored")
    opt[String](ClassName.full).abbr(ClassName.abbr).foreach(props.simulationClassName).text("Sets the name of the generated class")
    opt[String](Package.full).abbr(Package.abbr).foreach(props.simulationPackage).text("Sets the package of the generated class")
    opt[String](Encoding.full).abbr(Encoding.abbr).foreach(props.encoding).text("Sets the encoding used in the recorder")
    opt[Boolean](FollowRedirect.full).abbr(FollowRedirect.abbr).foreach(props.followRedirect).text("""Sets the "Follow Redirects" option to true""")
    opt[Boolean](AutomaticReferer.full).abbr(AutomaticReferer.abbr).foreach(props.automaticReferer).text("""Sets the "Automatic Referers" option to true""")
    opt[Boolean](FetchHtmlResources.full).abbr(FetchHtmlResources.abbr).foreach(props.fetchHtmlResources).text("""Sets the "Fetch html resources" option to true""")
  }

  def main(args: Array[String]) {
    if (cliOptsParser.parse(args))
      RecorderController(props.build)
  }
}
