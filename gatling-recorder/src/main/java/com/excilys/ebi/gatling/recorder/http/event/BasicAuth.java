package com.excilys.ebi.gatling.recorder.http.event;

public class BasicAuth {

	private String urlBase;
	private String username;
	private String password;

	public BasicAuth(String urlBase, String username, String password) {
		this.urlBase = urlBase;
		this.username = username;
		this.password = password;
	}

	public String getUrlBase() {
		return urlBase;
	}

	public void setUrlBase(String urlBase) {
		this.urlBase = urlBase;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
