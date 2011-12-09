/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.recorder.configuration;

import org.kohsuke.args4j.Option;

public class CommandLineOptions {
	
	@Option(name = "-lp", usage = "Local port", aliases = "--local-port")
	private int localPort = Configuration.getInstance().getPort();

	@Option(name = "-lps", usage = "Local SSL port", aliases = "--local-port-ssl")
	private int localPortSsl = Configuration.getInstance().getSslPort();

	@Option(name = "-ph", usage = "Outgoing proxy host", aliases = "--proxy-host")
	private String proxyHost = Configuration.getInstance().getProxy().getHost();

	@Option(name = "-pp", usage = "Outgoing proxy port", aliases = "--proxy-port")
	private int proxyPort = Configuration.getInstance().getProxy().getPort();

	@Option(name = "-pps", usage = "Outgoing proxy SSL port", aliases = "--proxy-port-ssl")
	private int proxyPortSsl = Configuration.getInstance().getProxy().getSslPort();

	@Option(name = "-of", usage = "Define the output folder for results", aliases = "--output-folder")
	private String outputFolder;

	@Option(name = "-text", usage = "Save scenario as Text")
	private boolean resultText;

	@Option(name = "-scala", usage = "Save scenario as Scala")
	private boolean resultScala;

	@Option(name = "-run", usage = "Skip the configuration frame (need to set -of, listens on 8000 & 8001)")
	private boolean runningFrame;
	
	@Option(name = "-eclipse", usage = "Eclipse & Archetype only")
	private boolean withEclipse;

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

	public boolean isResultText() {
		return resultText;
	}

	public void setResultText(boolean resultText) {
		this.resultText = resultText;
	}

	public boolean isResultScala() {
		return resultScala;
	}

	public void setResultScala(boolean resultScala) {
		this.resultScala = resultScala;
	}

	public boolean isRunningFrame() {
		return runningFrame;
	}

	public void setRunningFrame(boolean runningFrame) {
		this.runningFrame = runningFrame;
	}

	public boolean isWithEclipse() {
		return withEclipse;
	}

	public void setWithEclipse(boolean withEclipse) {
		this.withEclipse = withEclipse;
	}
}
