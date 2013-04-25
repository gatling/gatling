/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

	private val cliOptsParser = new OptionParser("gatling-recorder") {
		intOpt(LOCAL_PORT_OPTION, LOCAL_PORT_ALIAS, "<port>", "Local port used by Gatling Proxy for HTTP", props.localPort _)
		intOpt(LOCAL_PORT_SSL_OPTION, LOCAL_PORT_SSL_ALIAS, "<port>", "Local port used by Gatling Proxy for HTTPS", props.localSslPort _)
		opt(PROXY_HOST_OPTION, PROXY_HOST_ALIAS, "<host>", "Outgoing proxy host", props.proxyHost _)
		intOpt(PROXY_PORT_OPTION, PROXY_PORT_ALIAS, "<port>", "Outgoing proxy port for HTTP", props.proxyPort _)
		intOpt(PROXY_PORT_SSL_OPTION, PROXY_PORT_SSL_ALIAS, "<port>", "Outgoing proxy port for HTTPS", props.proxySslPort _)
		opt(OUTPUT_FOLDER_OPTION, OUTPUT_FOLDER_ALIAS, "<folderName>", "Uses <folderName> as the folder where generated simulations will be stored", props.simulationOutputFolder _)
		opt(REQUEST_BODIES_FOLDER_OPTION, REQUEST_BODIES_FOLDER_ALIAS, "<folderName>", "Uses <folderName> as the folder where request bodies are stored", props.requestBodiesFolder _)
		opt(CLASS_NAME_OPTION, CLASS_NAME_ALIAS, "Sets the name of the generated class", props.simulationClassName _)
		opt(PACKAGE_OPTION, PACKAGE_ALIAS, "Sets the package of the generated class", props.simulationPackage _)
		opt(ENCODING_OPTION, ENCODING_ALIAS, "Sets the encoding used in the recorder", props.encoding _)
		booleanOpt(FOLLOW_REDIRECT_OPTION, FOLLOW_REDIRECT_ALIAS, "Sets the follow redirect option to true", props.followRedirect _)
	}

	def main(args: Array[String]) {
		if (cliOptsParser.parse(args))
			RecorderController(props.build)
	}
}