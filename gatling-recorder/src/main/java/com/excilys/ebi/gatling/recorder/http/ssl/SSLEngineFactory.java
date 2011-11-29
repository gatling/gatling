package com.excilys.ebi.gatling.recorder.http.ssl;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.example.securechat.SecureChatSslContextFactory;

public class SSLEngineFactory {
	
	private SSLEngineFactory() {
		throw new UnsupportedOperationException();
	}

	public static SSLEngine newServerSSLEngine() {
		SSLEngine ctx = SecureChatSslContextFactory.getServerContext().createSSLEngine();
		ctx.setUseClientMode(false);
		return ctx;
	}

	public static SSLEngine newClientSSLEngine() {
		SSLEngine ctx = SecureChatSslContextFactory.getClientContext().createSSLEngine();
		ctx.setUseClientMode(true);
		return ctx;
	}
}
