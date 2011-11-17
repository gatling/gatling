package com.excilys.ebi.gatling.proxy.wrapper;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

public interface IHttpWrapper {
	public HttpRequest getHttpRequest();

	public HttpResponse getHttpResponse();
}
