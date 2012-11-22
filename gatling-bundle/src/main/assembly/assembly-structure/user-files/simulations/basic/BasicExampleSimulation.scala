package basic

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._

class BasicExampleSimulation extends Simulation {

	val httpConf = httpConfig
		.baseURL("http://excilys-bank-web.cloudfoundry.com")
		.acceptCharsetHeader("ISO-8859-1,utf-8;q=0.7,*;q=0.7")
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3")
		.disableFollowRedirect

	val headers_1 = Map(
		"Keep-Alive" -> "115")

	val headers_3 = Map(
		"Keep-Alive" -> "115",
		"Content-Type" -> "application/x-www-form-urlencoded")

	val headers_6 = Map(
		"Accept" -> "application/json, text/javascript, */*; q=0.01",
		"Keep-Alive" -> "115",
		"X-Requested-With" -> "XMLHttpRequest")

	val scn = scenario("Scenario name")
		.exec(
			http("request_1")
				.get("/")
				.headers(headers_1)
				.check(status.is(302)))
		.pause(0 milliseconds, 100 milliseconds)
		.exec(
			http("request_2")
				.get("/public/login.html")
				.headers(headers_1))
		.pause(12, 13)
		.feed(csv("user_information.csv"))
		.exec(
			http("request_3")
				.post("/login")
				.param("username", "${username}")
				.param("password", "${password}")
				.headers(headers_3)
				.check(status.is(302)))
		.pause(0 milliseconds, 100 milliseconds)
		.repeat(5) {
			exec(
				http("request_4")
					.get("/private/bank/accounts.html")
					.headers(headers_1))
				.pause(7, 8)
				.exec(
					http("request_5")
						.get("/private/bank/account/ACC${account_id}/operations.html")
						.headers(headers_1))
				.pause(100 milliseconds, 200 milliseconds)
				.exec(
					http("request_6")
						.get("/private/bank/account/ACC${account_id}/year/2011/month/12/page/0/operations.json")
						.headers(headers_6))
				.pause(4, 5)
				.exec(
					http("request_7")
						.get("/private/bank/account/ACC${account_id}/year/2011/month/11/operations.html")
						.headers(headers_1))
				.pause(100 milliseconds, 200 milliseconds)
				.exec(
					http("request_8")
						.get("/private/bank/account/ACC${account_id}/year/2011/month/11/page/0/operations.json")
						.headers(headers_6))
				.pause(6, 7)
		}.exec(
			http("request_9")
				.get("/logout")
				.headers(headers_1)
				.check(status.is(302)))
		.pause(0 milliseconds, 100 milliseconds)
		.exec(
			http("request_10")
				.get("/public/login.html")
				.headers(headers_1))

	run(scn.configure.users(10).ramp(10).protocolConfig(httpConf))
}
