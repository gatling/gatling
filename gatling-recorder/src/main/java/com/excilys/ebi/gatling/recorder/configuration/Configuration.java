package com.excilys.ebi.gatling.recorder.configuration;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.excilys.ebi.gatling.recorder.ui.enumeration.Filter;
import com.excilys.ebi.gatling.recorder.ui.enumeration.FilterType;
import com.excilys.ebi.gatling.recorder.ui.enumeration.ResultType;

public class Configuration {

	private int proxyPort;
	private String outgoingProxyHost;
	private int outgoingProxyPort;
	private Filter filter;
	private FilterType filterType;
	private List<String> filters;
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

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public FilterType getFilterType() {
		return filterType;
	}

	public void setFilterType(FilterType filterType) {
		this.filterType = filterType;
	}

	public List<String> getFilters() {
		return filters;
	}

	public void setFilters(List<String> filters) {
		this.filters = filters;
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
