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

import java.util.ArrayList;
import java.util.List;

import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.ResultType;

public class Configuration {

	public static Configuration getInstance() {
		return instance;
	}

	public static void initFromExistingConfig(Configuration c) {
		instance.setPort(c.getPort());
		instance.setSslPort(c.getSslPort());
		instance.setProxy(c.getProxy());
		instance.setFilterType(c.getFilterType());
		instance.setPatterns(c.getPatterns());
		instance.setOutputFolder(c.getOutputFolder());
		instance.setResultTypes(c.getResultTypes());
		instance.setSaveConfiguration(true);

	}

	public static void initFromCommandLineOptions(CommandLineOptions c) {
		instance.setPort(c.getLocalPort());
		instance.setSslPort(c.getLocalPortSsl());
		instance.getProxy().setHost(c.getProxyHost());
		instance.getProxy().setPort(c.getProxyPort());
		instance.getProxy().setSslPort(c.getProxyPortSsl());
		if (c.getOutputFolder() != null)
			instance.setOutputFolder(c.getOutputFolder());

		instance.getResultTypes().clear();
		if (c.isResultText())
			instance.getResultTypes().add(ResultType.TEXT);
		if (c.isResultScala())
			instance.getResultTypes().add(ResultType.SCALA);

		if (c.isRunningFrame())
			instance.setConfigurationSkipped(true);

		instance.setEclipsePackage(c.getEclipsePackage());
	}

	private static final Configuration instance = new Configuration();

	private int port = 8000;
	private int sslPort = 8001;
	private ProxyConfig proxy = new ProxyConfig();
	private FilterType filterType = FilterType.ALL;
	private List<Pattern> patterns = new ArrayList<Pattern>();
	private String outputFolder;
	private List<ResultType> resultTypes = new ArrayList<ResultType>();
	private boolean saveConfiguration;
	private transient boolean configurationSkipped;
	private transient String eclipsePackage;

	private Configuration() {

	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public ProxyConfig getProxy() {
		return proxy;
	}

	public void setProxy(ProxyConfig proxy) {
		this.proxy = proxy;
	}

	public FilterType getFilterType() {
		return filterType;
	}

	public void setFilterType(FilterType filterType) {
		this.filterType = filterType;
	}

	public List<Pattern> getPatterns() {
		return patterns;
	}

	public void setPatterns(List<Pattern> patterns) {
		this.patterns = patterns;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public List<ResultType> getResultTypes() {
		return resultTypes;
	}

	public void setResultTypes(List<ResultType> resultTypes) {
		this.resultTypes = resultTypes;
	}

	public int getSslPort() {
		return sslPort;
	}

	public void setSslPort(int sslPort) {
		this.sslPort = sslPort;
	}

	public boolean isSaveConfiguration() {
		return saveConfiguration;
	}

	public void setSaveConfiguration(boolean saveConfiguration) {
		this.saveConfiguration = saveConfiguration;
	}

	public boolean isConfigurationSkipped() {
		return configurationSkipped;
	}

	public void setConfigurationSkipped(boolean skipConfiguration) {
		this.configurationSkipped = skipConfiguration;
	}

	public String getEclipsePackage() {
		return eclipsePackage;
	}

	public void setEclipsePackage(String eclipsePackage) {
		this.eclipsePackage = eclipsePackage;
	}

	@Override
	public String toString() {
		return "Configuration [port=" + port + ", sslPort=" + sslPort + ", proxy=" + proxy + ", filterType=" + filterType + ", patterns=" + patterns + ", outputFolder=" + outputFolder
				+ ", resultTypes=" + resultTypes + ", saveConfiguration=" + saveConfiguration + ", configurationSkipped=" + configurationSkipped + "]";
	}
}
