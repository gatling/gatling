package org.gatling.example.script.custom

object Constants {
	val urlBase = "http://localhost:8080/excilys-bank-web"
	val urlLoginGet = urlBase + "/public/login.html"
	val urlLoginPost = urlBase + "/login"
	val urlHome = urlBase + "/private/bank/accounts.html"
	val urlLogout = urlBase + "/logout"
}