
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.script.GatlingSimulation

class Simulation extends GatlingSimulation {

	val PROTOCOL = "http"
	val HOST = "localhost" //0"192.168.10.142"
	val PORT = "8080"
	val urlBase = PROTOCOL + "://" + HOST + ":" + PORT

	val httpConf = httpConfig.baseURL(urlBase)

	val headers_2 = Map(
		"Accept" -> "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
		"Accept-Encoding" -> "gzip,deflate,sdch",
		"Accept-Language" -> "en-US,en;q=0.8,fr;q=0.6",
		"Cache-Control" -> "max-age=0",
		"Host" -> "192.168.10.142:8080",
		"Referer" -> "http://192.168.10.142:8080/excilys-bank-web/private/bank/account/ACC34/transfers/operations.html",
		"User-Agent" -> "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24"
		)

	val headers_4 = Map(
		"Accept" -> "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
		"Accept-Encoding" -> "gzip,deflate,sdch",
		"Accept-Language" -> "en-US,en;q=0.8,fr;q=0.6",
		"Cache-Control" -> "max-age=0",
		"Content-Length" -> "35",
		"Content-Type" -> "application/x-www-form-urlencoded",
		"Host" -> "192.168.10.142:8080",
		"Origin" -> "http://192.168.10.142:8080",
		"Referer" -> "http://192.168.10.142:8080/excilys-bank-web/public/login.html",
		"User-Agent" -> "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24"
		)

	val headers_6 = Map(
		"Accept" -> "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
		"Accept-Encoding" -> "gzip,deflate,sdch",
		"Accept-Language" -> "en-US,en;q=0.8,fr;q=0.6",
		"Cache-Control" -> "max-age=0",
		"Host" -> "192.168.10.142:8080",
		"Referer" -> "http://192.168.10.142:8080/excilys-bank-web/public/login.html",
		"User-Agent" -> "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24"
		)

	val headers_8 = Map(
		"Accept" -> "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
		"Accept-Encoding" -> "gzip,deflate,sdch",
		"Accept-Language" -> "en-US,en;q=0.8,fr;q=0.6",
		"Host" -> "192.168.10.142:8080",
		"Referer" -> "http://192.168.10.142:8080/excilys-bank-web/private/bank/accounts.html",
		"User-Agent" -> "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24"
		)

	val headers_10 = Map(
		"Accept" -> "application/json, text/javascript, */*; q=0.01",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
		"Accept-Encoding" -> "gzip,deflate,sdch",
		"Accept-Language" -> "en-US,en;q=0.8,fr;q=0.6",
		"Host" -> "192.168.10.142:8080",
		"Referer" -> "http://192.168.10.142:8080/excilys-bank-web/private/bank/account/ACC34/operations.html",
		"User-Agent" -> "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24",
		"X-Requested-With" -> "XMLHttpRequest"
		)

	val headers_12 = Map(
		"Accept" -> "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
		"Accept-Encoding" -> "gzip,deflate,sdch",
		"Accept-Language" -> "en-US,en;q=0.8,fr;q=0.6",
		"Host" -> "192.168.10.142:8080",
		"Referer" -> "http://192.168.10.142:8080/excilys-bank-web/private/bank/account/ACC34/operations.html",
		"User-Agent" -> "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24"
		)

	val headers_14 = Map(
		"Accept" -> "application/json, text/javascript, */*; q=0.01",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
		"Accept-Encoding" -> "gzip,deflate,sdch",
		"Accept-Language" -> "en-US,en;q=0.8,fr;q=0.6",
		"Host" -> "192.168.10.142:8080",
		"Referer" -> "http://192.168.10.142:8080/excilys-bank-web/private/bank/account/ACC34/transfers/operations.html",
		"User-Agent" -> "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24",
		"X-Requested-With" -> "XMLHttpRequest"
		)

	val headers_16 = Map(
		"Accept" -> "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
		"Accept-Encoding" -> "gzip,deflate,sdch",
		"Accept-Language" -> "en-US,en;q=0.8,fr;q=0.6",
		"Host" -> "192.168.10.142:8080",
		"Referer" -> "http://192.168.10.142:8080/excilys-bank-web/private/bank/account/ACC34/transfers/operations.html",
		"User-Agent" -> "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24"
		)

	val headers_18 = Map(
		"Accept" -> "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
		"Accept-Encoding" -> "gzip,deflate,sdch",
		"Accept-Language" -> "en-US,en;q=0.8,fr;q=0.6",
		"Cache-Control" -> "max-age=0",
		"Content-Length" -> "65",
		"Content-Type" -> "application/x-www-form-urlencoded",
		"Host" -> "192.168.10.142:8080",
		"Origin" -> "http://192.168.10.142:8080",
		"Referer" -> "http://192.168.10.142:8080/excilys-bank-web/private/bank/account/ACC34/transfers/perform.html",
		"User-Agent" -> "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24"
		)


	val headers_22 = Map(
		"Accept" -> "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
		"Accept-Encoding" -> "gzip,deflate,sdch",
		"Accept-Language" -> "en-US,en;q=0.8,fr;q=0.6",
		"Cache-Control" -> "max-age=0",
		"Host" -> "192.168.10.142:8080",
		"Referer" -> "http://192.168.10.142:8080/excilys-bank-web/private/bank/account/ACC34/transfers/perform.html",
		"User-Agent" -> "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24"
		)

	val headers_24 = Map(
		"Accept" -> "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5",
		"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
		"Accept-Encoding" -> "gzip,deflate,sdch",
		"Accept-Language" -> "en-US,en;q=0.8,fr;q=0.6",
		"Host" -> "192.168.10.142:8080",
		"Referer" -> "http://192.168.10.142:8080/excilys-bank-web/private/bank/accounts.html?accountNumber=ACC34&page=TRANSFER_PERFORM",
		"User-Agent" -> "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.71 Safari/534.24"
		)

val testData = tsv("bank_users.tsv")

val scn = scenario("Test Gatling vs JMeter")
	.feed(testData)
	.pause(0, 100, MILLISECONDS)
	.exec(
		http("Authentication page befor login")
		.get("/excilys-bank-web/public/login.html")
		.headers(headers_2)
		)
	.pause(5)
	.exec(
		http("Authentication")
		.post("/excilys-bank-web/login")
		.param("username", "${username}")
		.param("password", "${password}")
		.headers(headers_4)
		.check(status.eq(302))
		)
	.loop(
		chain
		.pause(0, 100, MILLISECONDS)
		.exec(
		http("Accounts")
		.get("/excilys-bank-web/private/bank/accounts.html")
		.headers(headers_6)
		.check(
			regex("""<a href="/excilys-bank-web/private/bank/account/(ACC[0-9]*)/operations.html">""").find(0).saveAs("acc1"),
			regex("""<a href="/excilys-bank-web/private/bank/account/(ACC[0-9]*)/operations.html">""").find(1).saveAs("acc2")
			)
		)
		.pause(5)
		.exec(
			http("Operations")
			.get("/excilys-bank-web/private/bank/account/${acc1}/operations.html")
			.headers(headers_8)
			)
		.pause(0, 100, MILLISECONDS)
		.exec(
			http("Operations data")
			.get("/excilys-bank-web/private/bank/account/${acc1}/year/2011/month/12/page/0/operations.json")
			.headers(headers_10)
			)
		.pause(5)
		.exec(
			http("Cards operations")
			.get("/excilys-bank-web/private/bank/account/${acc1}/transfers/operations.html")
			.headers(headers_12)
			)
		.pause(0, 100, MILLISECONDS)
		.exec(
			http("Cards operations data")
			.get("/excilys-bank-web/private/bank/account/${acc1}/transfers/page/0/operations.json")
			.headers(headers_14)
			)
		.pause(5)
		.exec(
			http("Perform transfer")
			.get("/excilys-bank-web/private/bank/account/${acc1}/transfers/perform.html")
			.headers(headers_16)
			)
		.pause(5)
		.exec(
			http("Performing transfer")
			.post("/excilys-bank-web/private/bank/account/${acc1}/transfers/perform.html")
			.param("debitedAccountNumber", "${acc1}")
			.param("creditedAccountNumber", "${acc2}")
			.param("amount", "100")
			.headers(headers_18)
			.check(status.eq(302))
			)
	).times(15)
	.pause(5)
	.exec(
		http("Logout")
		.get("/excilys-bank-web/logout")
		.headers(headers_24)
		.check(status.eq(302))
		)
	.pause(0, 100, MILLISECONDS)
	.exec(
		http("Authentication page after logout")
		.get("/excilys-bank-web/public/login.html")
		.headers(headers_24)
		)

	val scnConf = scn.configure users(1200) ramp(200) protocolConfig httpConf
	runSimulations(scnConf)

}
