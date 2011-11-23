package com.excilys.ebi.gatling.example.script.custom

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._

object Scenarios {

	/* URLs */
	val year = "2011"
	val month = "9"
	// Index starts at 0
	val pageOfOperations = "0"

	val headers= Map(
			"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
			"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
			"Accept-Language" -> "fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4",
			"Host" -> "localhost:8080",
			"Referer" -> "http://localhost:8080/excilys-bank-web/private/bank/account/ACC7/cards/CARD5/year/2011/month/7/operations.html",
			"User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.186 Safari/535.1")

	/* Feeder */
	val usersInfos = new TSVFeeder("bank2")
			
	/* Scenario */
	val scn = scenario("User of Excilys Bank")
		.feed(usersInfos)
		// Login page
		.exec(http("Login GET").get("/public/login.html").headers(headers).check(status.eq(200)))
		.pause(5, 6)
		// Authenticating
		.exec(http("Authenticating").post("/login").param("username").param("password").headers(headers).check(status.eq(302)))
		// Home page
		.exec(http("Home") get("/private/bank/accounts.html") headers(headers)
		    check(regexp("""<a href="/excilys-bank-web/private/bank/account/(ACC[0-9]*)/operations.html">""", 0) in "acc1")
		    check(regexp("""<a href="/excilys-bank-web/private/bank/account/(ACC[0-9]*)/operations.html">""", 1) in "acc2"))
		.pause(5, 6)
		.loop(
			chain
				// Operations page
				.exec(http("Operations details").get("/private/bank/account/${acc1}/operations.html").headers(headers).check(regexp("""<table class="accountDetails">""")))
				// Load operations data
				.exec(http("Operations data").get("/private/bank/account/${acc1}/year/" + year + "/month/" + month + "/page/" + pageOfOperations + "/operations.json").headers(headers).check(status.eq(200)))
				.pause(5, 6)

				// Cards operations page
				.exec(http("Cards details").get("/private/bank/account/${acc1}/cards/all/operations.html").headers(headers).check(regexp("""<table class="accountDetails">""")))
				// Load cards operations data
				.exec(http("Cards data").get( "/private/bank/account/${acc1}/cards/all/year/" + year + "/month/" + month + "/page/" + pageOfOperations + "/operations.json").check(status.eq(200)))
				.pause(5, 6)

				// Cards pending operations page
				.exec(http("Cards pending details").get("/private/bank/account/${acc1}/cards/all/pending/operations.html").headers(headers).check(regexp("""<table class="accountDetails">""")))
				// Load cards pending operations data
				.exec(http("Cards pending data").get("/private/bank/account/${acc1}/cards/all/pending/page/" + pageOfOperations + "/operations.json").headers(headers).check(status.eq(200)))
				.pause(5, 6)

				// Transfers page
				.exec(http("Transfers details").get("/private/bank/account/${acc1}/transfers/operations.html").headers(headers).check(regexp("""<table class="accountDetails">""")))
				// Load transfers data
				.exec(http("Transfers data").get("/private/bank/account/${acc1}/transfers/page/" + pageOfOperations + "/operations.json").headers(headers).check(status.eq(200)))
				.pause(5, 6)

				// Transfer perform page
				.exec(http("Transfer perform").get("/private/bank/account/${acc1}/transfers/perform.html").headers(headers))
				.pause(5, 6)

				// Transfer performing
				.exec(http("Transfer performing") post("/private/bank/account/${acc1}/transfers/perform.html")
					param("debitedAccountNumber", "${acc1}")
					param("creditedAccountNumber", "${acc2}")
					param("amount", "10")
					headers(headers)
					check(status.eq(302)))
				.pause(5, 6))
		.times(20)

		// Logout
		.exec(http("Logging out").get("/logout").headers(headers).check(status.eq(302)))
}