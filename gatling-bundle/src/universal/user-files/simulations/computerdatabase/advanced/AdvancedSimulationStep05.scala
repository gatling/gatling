package computerdatabase.advanced

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.util.concurrent.ThreadLocalRandom

class AdvancedSimulationStep05 extends Simulation {

  object Search {

    val feeder = csv("search.csv").random

    val search = exec(http("Home")
       .get("/"))
      .pause(1)
      .feed(feeder)
      .exec(http("Search")
        .get("/computers")
        .queryParam("""f""", "${searchCriterion}")
        .check(regex("""<a href="([^"]+)">${searchComputerName}</a>""").saveAs("computerURL")))
      .pause(1)
      .exec(http("Select")
        .get("${computerURL}")
        .check(status.is(200)))
      .pause(1)
  }

  object Browse {

	  // repeat is a loop resolved at RUNTIME
	  val browse = repeat(4, "i") { // Note how we force the counter name so we can reuse it
		  exec(http("Page ${i}")
			  .get("/computers")
			  .queryParam("""p""", "${i}"))
			  .pause(1)
	  }
  }

  object Edit {

    // Note we should be using a feeder here

    val headers_10 = Map("Content-Type" -> """application/x-www-form-urlencoded""")

    // let's demonstrate how we can retry: let's make the request fail randomly and retry a given number of times

    val edit = tryMax(2) { // let's try at max 2 times
      exec(http("Form")
        .get("/computers/new"))
      .pause(1)
      .exec(http("Post")
        .post("/computers")
        .headers(headers_10)
        .param("""name""", """Beautiful Computer""")
        .param("""introduced""", """2012-05-30""")
        .param("""discontinued""", """""")
        .param("""company""", """37""").
        check(status.is(session => 200 + ThreadLocalRandom.current.nextInt(2)))) // we do a check on a condition that's been customized with a lambda. It will be evaluated every time a user executes the request
    }.exitHereIfFailed // if the chain didn't finally succeed, have the user exit the whole scenario
  }

  val httpConf = http
    .baseURL("http://computer-database.herokuapp.com")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val users = scenario("Users").exec(Search.search, Browse.browse)
  val admins = scenario("Admins").exec(Search.search, Browse.browse, Edit.edit)

  setUp(
    users.inject(rampUsers(10) over (10 seconds)),
    admins.inject(rampUsers(2) over (10 seconds))
  ).protocols(httpConf)
}
