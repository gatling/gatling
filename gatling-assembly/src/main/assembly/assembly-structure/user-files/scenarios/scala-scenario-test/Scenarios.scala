package org.gatling.example.script.custom

import org.gatling.example.script.custom.Constants._

import com.excilys.ebi.gatling.core.feeder._
import com.excilys.ebi.gatling.core.context._
import com.excilys.ebi.gatling.core.util.StringHelper._
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder._
import com.excilys.ebi.gatling.core.scenario.builder.ChainBuilder._
import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpRegExpCaptureBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpXPathCaptureBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpHeaderCaptureBuilder._
import com.excilys.ebi.gatling.http.processor.check.builder.HttpXPathCheckBuilder._
import com.excilys.ebi.gatling.http.processor.check.builder.HttpRegExpCheckBuilder._
import com.excilys.ebi.gatling.http.processor.check.builder.HttpStatusCheckBuilder._
import com.excilys.ebi.gatling.http.processor.check.builder.HttpHeaderCheckBuilder._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime

object Scenarios {

	/* URLs */
	val year = "2011"
	val month = "9"
	// Index starts at 0
	val pageOfOperations = "0"

	val urlAccountOperations = urlBase + "/private/bank/account/{}/operations.html"
	val urlAccountOperationsData = urlBase + "/private/bank/account/{}/year/" + year + "/month/" + month + "/page/" + pageOfOperations + "/operations.json"

	val urlAccountCards = urlBase + "/private/bank/account/{}/cards/all/operations.html"
	val urlAccountCardsData = urlBase + "/private/bank/account/{}/cards/all/year/" + year + "/month/" + month + "/page/" + pageOfOperations + "/operations.json"

	val urlAccountCardsPending = urlBase + "/private/bank/account/{}/cards/all/pending/operations.html"
	val urlAccountCardsPendingData = urlBase + "/private/bank/account/{}/cards/all/pending/page/" + pageOfOperations + "/operations.json"

	val urlAccountTransfers = urlBase + "/private/bank/account/{}/transfers/operations.html"
	val urlAccountTransfersData = urlBase + "/private/bank/account/{}/transfers/page/" + pageOfOperations + "/operations.json"

	val urlAccountTransferPerform = urlBase + "/private/bank/account/{}/transfers/perform.html"

	val headers= Map(
			"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
			"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
			"Accept-Language" -> "fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4",
			"Host" -> "localhost:8080",
			"Referer" -> "http://localhost:8080/excilys-bank-web/private/bank/account/ACC7/cards/CARD5/year/2011/month/7/operations.html",
			"User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.186 Safari/535.1")

	/* Scenario */
	val scn = scenario("User of Excilys Bank")
		// Login page
		.exec(http("Login GET") get(urlLoginGet) headers(headers) check(status(200)))
		.pause(5, 6)
		// Authenticating
		.exec(http("Authenticating") post (urlLoginPost) param ("username") param ("password") headers(headers) check(status(302)))
		// Home page
		.exec(http("Home") get(urlHome) headers(headers) check(regexpExists("""<a href="/excilys-bank-web/logout" class="button blue">Log out</a>""")))
		.pause(5, 6)
		.iterate(
			20,
			chain
				// Operations page
				.exec(http("Operations details") get(urlAccountOperations, "acc1") headers(headers) check(regexpExists("""<table class="accountDetails">""")))
				// Load operations data
				.exec(http("Operations data") get(urlAccountOperationsData, "acc1") headers(headers) check(status(200)))
				.pause(5, 6)

				// Cards operations page
				.exec(http("Cards details") get(urlAccountCards, "acc1") headers(headers) check(regexpExists("""<table class="accountDetails">""")))
				// Load cards operations data
				.exec(http("Cards data") get(urlAccountCardsData, "acc1") check(status(200)))
				.pause(5, 6)

				// Cards pending operations page
				.exec(http("Cards pending details") get(urlAccountCardsPending, "acc1") headers(headers) check(regexpExists("""<table class="accountDetails">""")))
				// Load cards pending operations data
				.exec(http("Cards pending data") get(urlAccountCardsPendingData, "acc1") headers(headers) check(status(200)))
				.pause(5, 6)

				// Transfers page
				.exec(http("Transfers details") get(urlAccountTransfers, "acc1") headers(headers) check(regexpExists("""<table class="accountDetails">""")))
				// Load transfers data
				.exec(http("Transfers data") get(urlAccountTransfersData, "acc1") headers(headers) check(status(200)))
				.pause(5, 6)

				// Transfer perform page
				.exec(http("Transfer perform") get(urlAccountTransferPerform, "acc1") headers(headers))
				.pause(5, 6)

				// Transfer performing
				.exec(http("Transfer performing") post(urlAccountTransferPerform, "acc1")
					param ("debitedAccountNumber", FromContext("acc1"))
					param ("creditedAccountNumber", FromContext("acc2"))
					param ("amount", "10")
					headers(headers)
					check(status(302)))
				.pause(5, 6))

		// Logout
		.exec(http("Logging out") get(urlLogout) headers(headers) check(status(302)))
}