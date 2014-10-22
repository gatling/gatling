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

  private val cliOptsParser = new OptionParser[Unit]("gatling-recorder") with CommandLineConstantsSupport[Unit] {
    help(Help).text("Show help (this message) and exit")
    opt[Int](LocalPort).valueName("<port>").foreach(props.localPort).text("Local port used by Gatling Proxy for HTTP/HTTPS")
    opt[String](ProxyHost).valueName("<host>").foreach(props.proxyHost).text("Outgoing proxy host")
    opt[Int](ProxyPort).valueName("<port>").foreach(props.proxyPort).text("Outgoing proxy port for HTTP")
    opt[Int](ProxyPortSsl).valueName("<port>").foreach(props.proxySslPort).text("Outgoing proxy port for HTTPS")
    opt[String](OutputFolder).valueName("<folderName>").foreach(props.simulationOutputFolder).text("Uses <folderName> as the folder where generated simulations will be stored")
    opt[String](RequestBodiesFolder).valueName("<folderName>").foreach(props.requestBodiesFolder).text("Uses <folderName> as the folder where request bodies are stored")
    opt[String](ClassName).foreach(props.simulationClassName).text("Sets the name of the generated class")
    opt[String](SuperClassName).foreach(props.simulationSuperClassName).text("Sets the superclass of the generated class")
    opt[String](Package).foreach(props.simulationPackage).text("Sets the package of the generated class")
    opt[String](Encoding).foreach(props.encoding).text("Sets the encoding used in the recorder")
    opt[Boolean](FollowRedirect).foreach(props.followRedirect).text("""Sets the "Follow Redirects" option to true""")
    opt[Boolean](AutomaticReferer).foreach(props.automaticReferer).text("""Sets the "Automatic Referers" option to true""")
    opt[Boolean](InferHtmlResources).foreach(props.inferHtmlResources).text("""Sets the "Fetch html resources" option to true""")
  }

  def main(args: Array[String]): Unit = {
    if (cliOptsParser.parse(args))
      RecorderController(props.build)
  }
}
