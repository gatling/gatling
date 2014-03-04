package computerdatabase.advanced

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class AdvancedSimulationStep04 extends Simulation {

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

    def gotoPage(page: String) = exec(http("Page " + page)
        .get("/computers")
        .queryParam("""p""", page))
      .pause(1)

    def gotoUntil(max: String) = repeat(max.toInt, "i") { // repeat is a loop resolved at RUNTIME
      gotoPage("${i}") // Note how we force the counter name so we can reuse it
    }

    // As the max page is static and doesn't depend on the user, we might as well have a loop resolved at LOAD time
    def gotoUntil2(max: String) = exec(for (i <- 0 until max.toInt) yield gotoPage(i.toString))

    val browse = gotoUntil2("4")
  }

  object Edit {

    val headers_10 = Map("Content-Type" -> """application/x-www-form-urlencoded""")

    val edit = exec(http("Form")
        .get("/computers/new"))
      .pause(1)
      .exec(http("Post")
        .post("/computers")
        .headers(headers_10)
        .param("""name""", """Beautiful Computer""")
        .param("""introduced""", """2012-05-30""")
        .param("""discontinued""", """""")
        .param("""company""", """37"""))
  }

  val httpConf = http
    .baseURL("http://computer-database.heroku.com/")
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
