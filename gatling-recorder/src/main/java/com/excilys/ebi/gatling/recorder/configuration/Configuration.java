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

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.ResultType;

public class Configuration {

	private int proxyPort;
	private String outgoingProxyHost;
	private int outgoingProxyPort;
	private FilterType filterType;
	private List<Pattern> patterns;
	private String resultPath;
	private List<ResultType> resultTypes;

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getOutgoingProxyHost() {
		return outgoingProxyHost;
	}

	public void setOutgoingProxyHost(String outgoingProxyHost) {
		this.outgoingProxyHost = StringUtils.trimToNull(outgoingProxyHost);
	}

	public int getOutgoingProxyPort() {
		return outgoingProxyPort;
	}

	public void setOutgoingProxyPort(int outgoingProxyPort) {
		this.outgoingProxyPort = outgoingProxyPort;
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

	public String getResultPath() {
		return resultPath;
	}

	public void setResultPath(String resultPath) {
		this.resultPath = StringUtils.trimToNull(resultPath);
	}

	public List<ResultType> getResultTypes() {
		return resultTypes;
	}

	public void setResultTypes(List<ResultType> resultTypes) {
		this.resultTypes = resultTypes;
	}
}
