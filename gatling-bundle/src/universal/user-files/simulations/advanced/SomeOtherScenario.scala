package advanced

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import Headers._

object SomeOtherScenario {

	val otherScn = scenario("Other Scenario Name")
		.exec(
			http("other_request_1")
				.get("/")
				.check(status.is(302)))
		.pause(0 milliseconds, 100 milliseconds)
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
		.pause(0 milliseconds, 100 milliseconds)
		.exec(
			http("other_request_9")
				.get("/logout")
				.headers(headers_1)
				.check(status.is(302)))
		.pause(0 milliseconds, 100 milliseconds)
		.exec(
			http("other_request_10")
				.get("/public/login.html"))
}
