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
package com.excilys.ebi.gatling.recorder

import com.excilys.ebi.gatling.recorder.config.RecorderOptions
import com.excilys.ebi.gatling.recorder.controller.RecorderController

import scopt.OptionParser

object GatlingRecorder {

	val LOCAL_PORT_OPTION = "lp"
	val LOCAL_PORT_ALIAS = "local-port"
	val LOCAL_PORT_SSL_OPTION = "lps"
	val LOCAL_PORT_SSL_ALIAS = "local-port-ssl"
	val PROXY_HOST_OPTION = "ph"
	val PROXY_HOST_ALIAS = "proxy-host"
	val PROXY_PORT_OPTION = "pp"
	val PROXY_PORT_ALIAS = "proxy_port"
	val PROXY_PORT_SSL_OPTION = "pps"
	val PROXY_PORT_SSL_ALIAS = "proxy-port-ssl"
	val OUTPUT_FOLDER_OPTION = "of"
	val OUTPUT_FOLDER_ALIAS = "output-folder"
	val REQUEST_BODIES_FOLDER_OPTION = "rbf"
	val REQUEST_BODIES_FOLDER_ALIAS = "request-bodies-folder"
	val CLASS_NAME_OPTION = "cn"
	val CLASS_NAME_ALIAS = "class-name"
	val PACKAGE_OPTION = "pkg"
	val PACKAGE_ALIAS = "package"
	val ENCODING_OPTION = "enc"
	val ENCODING_ALIAS = "encoding"
	val FOLLOW_REDIRECT_OPTION = "fr"
	val FOLLOW_REDIRECT_ALIAS = "follow-redirect"

	private val o = new RecorderOptions

	private val cliOptsParser = new OptionParser("gatling-recorder") {
		intOpt(LOCAL_PORT_OPTION, LOCAL_PORT_ALIAS, "<port>", "Local port used by Gatling Proxy for HTTP", { v: Int => o.localPort = Some(v) })
		intOpt(LOCAL_PORT_SSL_OPTION, LOCAL_PORT_SSL_ALIAS, "<port>", "Local port used by Gatling Proxy for HTTPS", { v: Int => o.localPortSsl = Some(v) })
		opt(PROXY_HOST_OPTION, PROXY_HOST_ALIAS, "<host>", "Outgoing proxy host", { v: String => o.proxyHost = Some(v) })
		intOpt(PROXY_PORT_OPTION, PROXY_PORT_ALIAS, "<port>", "Outgoing proxy port for HTTP", { v: Int => o.proxyPort = Some(v) })
		intOpt(PROXY_PORT_SSL_OPTION, PROXY_PORT_SSL_ALIAS, "<port>", "Outgoing proxy port for HTTPS", { v: Int => o.proxyPortSsl = Some(v) })
		opt(OUTPUT_FOLDER_OPTION, OUTPUT_FOLDER_ALIAS, "<folderName>", "Uses <folderName> as the folder where generated simulations will be stored", { v: String => o.outputFolder = Some(v) })
		opt(REQUEST_BODIES_FOLDER_OPTION, REQUEST_BODIES_FOLDER_ALIAS, "<folderName>", "Uses <folderName> as the folder where request bodies are stored", { v: String => o.requestBodiesFolder = Some(v) })
		opt(CLASS_NAME_OPTION, CLASS_NAME_ALIAS, "Sets the name of the generated class", { v: String => o.simulationClassName = Some(v) })
		opt(PACKAGE_OPTION, PACKAGE_ALIAS, "Sets the package of the generated class", { v: String => o.simulationPackage = Some(v) })
		opt(ENCODING_OPTION, ENCODING_ALIAS, "Sets the encoding used in the recorder", { v: String => o.encoding = Some(v) })
		booleanOpt(FOLLOW_REDIRECT_OPTION, FOLLOW_REDIRECT_ALIAS, "Sets the follow redirect option to true", { v: Boolean => o.followRedirect = Some(v) })
	}

	def main(args: Array[String]) {
		if (cliOptsParser.parse(args))
			RecorderController(o)
	}
}