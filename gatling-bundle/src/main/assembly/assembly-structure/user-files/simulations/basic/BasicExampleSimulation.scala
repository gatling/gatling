package basic
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.app.Simulation

class BasicExampleSimulation extends Simulation {

	def apply = {

		val urlBase = "http://excilys-bank-web.cloudfoundry.com"

		val httpConf = httpConfig.baseURL(urlBase)

		val headers_1 = Map(
			"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
			"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
			"Accept-Encoding" -> "gzip,deflate",
			"Accept-Language" -> "fr,en-us;q=0.7,en;q=0.3",
			"Host" -> "excilys-bank-web.cloudfoundry.com",
			"Keep-Alive" -> "115",
			"User-Agent" -> "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110422 Ubuntu/9.10 (karmic) Firefox/3.6.17")

		val headers_3 = headers_1 ++ Map(
			"Content-Length" -> "33",
			"Content-Type" -> "application/x-www-form-urlencoded",
			"Referer" -> "http://excilys-bank-web.cloudfoundry.com/public/login.html")

		val headers_4 = headers_1 ++ Map(
			"Referer" -> "http://excilys-bank-web.cloudfoundry.com/public/login.html")

		val headers_5 = headers_1 ++ Map(
			"Referer" -> "http://excilys-bank-web.cloudfoundry.com/private/bank/accounts.html")

		val headers_6 = Map(
			"Accept" -> "application/json, text/javascript, */*; q=0.01",
			"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
			"Accept-Encoding" -> "gzip,deflate",
			"Accept-Language" -> "fr,en-us;q=0.7,en;q=0.3",
			"Host" -> "excilys-bank-web.cloudfoundry.com",
			"Keep-Alive" -> "115",
			"Referer" -> "http://excilys-bank-web.cloudfoundry.com/private/bank/account/ACC4/operations.html",
			"User-Agent" -> "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110422 Ubuntu/9.10 (karmic) Firefox/3.6.17",
			"X-Requested-With" -> "XMLHttpRequest")

		val headers_7 = headers_1 ++ Map(
			"Referer" -> "http://excilys-bank-web.cloudfoundry.com/private/bank/account/ACC4/operations.html")

		val headers_8 = Map(
			"Accept" -> "application/json, text/javascript, */*; q=0.01",
			"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
			"Accept-Encoding" -> "gzip,deflate",
			"Accept-Language" -> "fr,en-us;q=0.7,en;q=0.3",
			"Host" -> "excilys-bank-web.cloudfoundry.com",
			"Keep-Alive" -> "115",
			"Referer" -> "http://excilys-bank-web.cloudfoundry.com/private/bank/account/ACC4/year/2011/month/11/operations.html",
			"User-Agent" -> "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110422 Ubuntu/9.10 (karmic) Firefox/3.6.17",
			"X-Requested-With" -> "XMLHttpRequest")

		val headers_9 = headers_1 ++ Map(
			"Referer" -> "http://excilys-bank-web.cloudfoundry.com/private/bank/account/ACC4/year/2011/month/11/operations.html")

		val scn = scenario("Scenario name")
			.exec(
				http("request_1")
					.get("/")
					.headers(headers_1)
					.check(status.is(302)))
			.pause(0, 100, MILLISECONDS)
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
			.pause(0, 100, MILLISECONDS)
			.loop(
				chain
					.exec(
						http("request_4")
							.get("/private/bank/accounts.html")
							.headers(headers_4))
					.pause(7, 8)
					.exec(
						http("request_5")
							.get("/private/bank/account/ACC${account_id}/operations.html")
							.headers(headers_5))
					.pause(100, 200, MILLISECONDS)
					.exec(
						http("request_6")
							.get("/private/bank/account/ACC${account_id}/year/2011/month/12/page/0/operations.json")
							.headers(headers_6))
					.pause(4, 5)
					.exec(
						http("request_7")
							.get("/private/bank/account/ACC${account_id}/year/2011/month/11/operations.html")
							.headers(headers_7))
					.pause(100, 200, MILLISECONDS)
					.exec(
						http("request_8")
							.get("/private/bank/account/ACC${account_id}/year/2011/month/11/page/0/operations.json")
							.headers(headers_8))
					.pause(6, 7)).times(5)
			.exec(
				http("request_9")
					.get("/logout")
					.headers(headers_9)
					.check(status.is(302)))
			.pause(0, 100, MILLISECONDS)
			.exec(
				http("request_10")
					.get("/public/login.html")
					.headers(headers_9))

		val scnConf = scn.configure users 10 ramp 10 protocolConfig httpConf
		List(scnConf)
	}
}
