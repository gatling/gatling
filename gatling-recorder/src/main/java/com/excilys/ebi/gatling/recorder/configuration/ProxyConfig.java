package com.excilys.ebi.gatling.recorder.configuration;

public class ProxyConfig {

	private String host;
	private int port;
	private int sslPort;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getSslPort() {
		return sslPort;
	}

	public void setSslPort(int sslPort) {
		this.sslPort = sslPort;
	}

	@Override
	public String toString() {
		return "ProxyConfig [host=" + host + ", port=" + port + ", sslPort=" + sslPort + "]";
	}
}
