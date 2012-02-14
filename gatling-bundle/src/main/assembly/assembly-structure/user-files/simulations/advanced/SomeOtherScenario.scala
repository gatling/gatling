package advanced
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import Headers._

object SomeOtherScenario {

	val otherScn = scenario("Other Scenario Name")
		.exec(
			http("other_request_1")
				.get("/")
				.check(status.is(302)))
		.pause(0, 100, MILLISECONDS)
		.exec(
			http("other_request_2")
				.get("/public/login.html"))
		.pause(12, 13)
		.feed(csv("user_credentials.csv"))
		.exec(
			http("other_request_3")
				.post("/login")
				.param("username", "${username}")
				.param("password", "${password}")
				.check(status.is(302)))
		.pause(0, 100, MILLISECONDS)
		.exec(
			http("other_request_9")
				.get("/logout")
				.headers(headers_9)
				.check(status.is(302)))
		.pause(0, 100, MILLISECONDS)
		.exec(
			http("other_request_10")
				.get("/public/login.html"))
}
