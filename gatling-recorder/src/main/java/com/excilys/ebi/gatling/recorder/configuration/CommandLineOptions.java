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
package com.excilys.ebi.gatling.recorder.configuration;

import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.ENCODING_ALIAS;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.ENCODING_OPTION;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.LOCAL_HTTPS_PORT_ALIAS;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.LOCAL_HTTPS_PORT_OPTION;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.LOCAL_HTTP_PORT_ALIAS;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.LOCAL_HTTP_PORT_OPTION;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.OUTPUT_FOLDER_ALIAS;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.OUTPUT_FOLDER_OPTION;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.PACKAGE_ALIAS;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.PACKAGE_OPTION;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.PROXY_HOST_ALIAS;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.PROXY_HOST_OPTION;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.PROXY_HTTPS_PORT_ALIAS;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.PROXY_HTTPS_PORT_OPTION;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.PROXY_HTTP_PORT_ALIAS;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.PROXY_HTTP_PORT_OPTION;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.REQUEST_BODIES_FOLDER_ALIAS;
import static com.excilys.ebi.gatling.recorder.configuration.CommandLineOptionsConstants.REQUEST_BODIES_FOLDER_OPTION;

import org.kohsuke.args4j.Option;

public class CommandLineOptions {

	@Option(name = LOCAL_HTTP_PORT_OPTION, usage = "Local port", aliases = LOCAL_HTTP_PORT_ALIAS)
	private int localPort = Configuration.getInstance().getPort();

	@Option(name = LOCAL_HTTPS_PORT_OPTION, usage = "Local SSL port", aliases = LOCAL_HTTPS_PORT_ALIAS)
	private int localPortSsl = Configuration.getInstance().getSslPort();

	@Option(name = PROXY_HOST_OPTION, usage = "Outgoing proxy host", aliases = PROXY_HOST_ALIAS)
	private String proxyHost = Configuration.getInstance().getProxy().getHost();

	@Option(name = PROXY_HTTP_PORT_OPTION, usage = "Outgoing proxy port", aliases = PROXY_HTTP_PORT_ALIAS)
	private int proxyPort = Configuration.getInstance().getProxy().getPort();

	@Option(name = PROXY_HTTPS_PORT_OPTION, usage = "Outgoing proxy SSL port", aliases = PROXY_HTTPS_PORT_ALIAS)
	private int proxyPortSsl = Configuration.getInstance().getProxy().getSslPort();

	@Option(name = OUTPUT_FOLDER_OPTION, usage = "Define the output folder for results", aliases = OUTPUT_FOLDER_ALIAS)
	private String outputFolder;

	@Option(name = REQUEST_BODIES_FOLDER_OPTION, usage = "Define the folder in which the request bodies will be dumped", aliases = REQUEST_BODIES_FOLDER_ALIAS)
	private String requestBodiesFolder;

	@Option(name = PACKAGE_OPTION, usage = "Set the package of the generated Simulation class", aliases = PACKAGE_ALIAS)
	private String idePackage;

	@Option(name = ENCODING_OPTION, usage = "Set the encoding for file operations", aliases = ENCODING_ALIAS)
	private String encoding;

	public int getLocalPort() {
		return localPort;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	public int getLocalPortSsl() {
		return localPortSsl;
	}

	public void setLocalPortSsl(int localPortSsl) {
		this.localPortSsl = localPortSsl;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public int getProxyPortSsl() {
		return proxyPortSsl;
	}

	public void setProxyPortSsl(int proxyPortSsl) {
		this.proxyPortSsl = proxyPortSsl;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public String getIdePackage() {
		return idePackage;
	}

	public void setIdePackage(String idePackage) {
		this.idePackage = idePackage;
	}

	public String getRequestBodiesFolder() {
		return requestBodiesFolder;
	}

	public void setRequestBodiesFolder(String requestBodiesFolder) {
		this.requestBodiesFolder = requestBodiesFolder;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
}
