package advanced
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import Headers._

object SomeScenario {

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
		.feed(csv("user_credentials.csv"))
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
						.headers(headers_4)
						.check(regex("""<td class="number">ACC(\d+)</td>""").saveAs("account_id")))
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
		.doIf((session: Session) => session.getAttribute("username") != "user7",
			chain
				.exec(
					http("request_9")
						.get("/logout")
						.headers(headers_9)
						.check(status.is(302)))
				.pause(0, 100, MILLISECONDS)
				.exec(
					http("request_10")
						.get("/public/login.html")
						.headers(headers_9)))
}