package com.excilys.ebi.gatling.recorder.ui.event;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpEvent implements Event {

	private String name;
	private String url;
	private String method;
	private String content;
	private Map<String, List<String>> params;
	private String headers;

	private String stringRequest;
	private String stringResponse;

	public HttpEvent() {
		name = null;
		url = null;
		method = null;
		content = null;
		params = new LinkedHashMap<String, List<String>>();
		headers = null;
		stringRequest = null;
		stringResponse = null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Map<String, List<String>> getParams() {
		return params;
	}

	public void setParams(Map<String, List<String>> params) {
		this.params = params;
	}

	public String getHeaders() {
		return headers;
	}

	public void setHeaders(String headers) {
		this.headers = headers;
	}

	public String getStringRequest() {
		return stringRequest;
	}

	public void setStringRequest(String request) {
		this.stringRequest = request;
	}

	public String getStringResponse() {
		return stringResponse;
	}

	public void setStringResponse(String response) {
		this.stringResponse = response;
	}
}