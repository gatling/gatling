package com.excilys.ebi.gatling.proxy.event;

import java.util.*;

import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;

@SuppressWarnings("serial")
public class ProxyEvent extends EventObject {

	private HttpRequest originalRequest;
	private int id;
	private Map<String, List<String>> requestParams = new LinkedHashMap<String, List<String>>();
	private boolean withBody;

	public ProxyEvent(HttpMessage source) {
		super(source);
	}

	public ProxyEvent(HttpMessage source, HttpRequest correspondingRequest) {
		this(source);
		this.originalRequest = correspondingRequest;
	}

	public HttpRequest getOriginalRequest() {
		return originalRequest;
	}

	public void setOriginalRequest(HttpRequest originalRequest) {
		this.originalRequest = originalRequest;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Map<String, List<String>> getRequestParams() {
		return requestParams;
	}

	public void setRequestParams(Map<String, List<String>> requestParams) {
		this.requestParams = requestParams;
	}

	public boolean isWithBody() {
		return withBody;
	}

	public void setWithBody(boolean withBody) {
		this.withBody = withBody;
	}

}
