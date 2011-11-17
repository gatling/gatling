package com.excilys.ebi.gatling.recorder.http.event;

public interface ProxyListener {

	void onHttpRequest(ProxyEvent e);

	void onHttpResponse(ProxyEvent e);
}
