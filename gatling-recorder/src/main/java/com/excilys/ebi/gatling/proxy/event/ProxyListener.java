package com.excilys.ebi.gatling.proxy.event;

public interface ProxyListener {

	void onHttpRequest(ProxyEvent e);

	void onHttpResponse(ProxyEvent e);
}
