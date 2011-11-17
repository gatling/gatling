package com.excilys.ebi.gatling.recorder.wrapper;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class HttpWrapper implements IHttpWrapper {
	private HttpRequest request;
	private HttpResponse response;

	public HttpWrapper(HttpRequest req) {
		this.request = req;
	}

	public HttpWrapper(HttpRequest req, HttpResponse resp) {
		this(req);
		setHttpResponse(resp);
	}

	public void setHttpResponse(HttpResponse resp) {
		this.response = resp;
	}

	@Override
	public HttpRequest getHttpRequest() {
		return this.request;
	}

	@Override
	public HttpResponse getHttpResponse() {
		return this.response;
	}
}
