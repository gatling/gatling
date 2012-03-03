/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import com.excilys.ebi.gatling.recorder.config.Options
import com.excilys.ebi.gatling.recorder.controller.RecorderController

import scopt.OptionParser

object GatlingRecorder extends App {
	
	val o = new Options()

	val cliOptsParser = new OptionParser("gatling-recorder") {
		intOpt("lp", "local-port", "<port>", "Local port used by Gatling Proxy for HTTP", { v: Int => o.localPort = Some(v)})
		intOpt("lps", "local-port-ssl", "<port>", "Local port used by Gatling Proxy for HTTPS", { v: Int => o.localPortSsl = Some(v) })
		opt("ph", "proxy-host", "<host>", "Outgoing proxy host", { v: String => o.proxyHost = Some(v) })
		intOpt("pp", "proxy-port", "<port>", "Outgoing proxy port for HTTP", { v: Int => o.proxyPort = Some(v) })
		intOpt("pps", "proxy-port-ssl", "<port>", "Outgoing proxy port for HTTPS", { v: Int => o.proxyPortSsl = Some(v) })
		opt("of", "ouput-folder", "<folderName>", "Uses <folderName> as the folder where generated simulations will be stored", { v: String => o.outputFolder = Some(v) })
		opt("bf", "request-bodies-folder", "<folderName>", "Uses <folderName> as the folder where request bodies are stored", { v: String => o.requestBodiesFolder = Some(v) })
		opt("cn", "class-name", "Sets the name of the generated class", { v: String => o.simulationClassName = Some(v) })
		opt("pkg", "package", "Sets the package of the generated class", { v: String => o.simulationPackage = Some(v)})
		opt("enc",  "encoding", "Sets the encoding used in the recorder", {v:String => o.encoding = Some(v)})
		booleanOpt("fr", "follow-redirect", "Sets the follow redirect option to true", {v: Boolean => o.followRedirect = Some(v)})
	}

	if (cliOptsParser.parse(args))
		RecorderController(o)
}

		