package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class BasicSimulation extends Simulation {

  val httpConf = http
    .baseURL("http://computer-database.heroku.com/") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val headers_10 = Map("Content-Type" -> """application/x-www-form-urlencoded""") // Note the headers specific to a given request

  val scn = scenario("Scenario Name") // A scenario is a chain of requests and pauses
    .exec(http("request_1")
      .get("/"))
    .pause(7) // Note that Gatling has recorder real time pauses
    .exec(http("request_2")
      .get("/computers")
      .queryParam("""f""", """macbook""")) // Note the triple double quotes: used in Scala for protecting a whole chain of characters (no need for backslash)
    .pause(2)
    .exec(http("request_3")
      .get("/computers/6"))
    .pause(3)
    .exec(http("request_4")
      .get("/"))
    .pause(2)
    .exec(http("request_5")
      .get("/computers")
      .queryParam("""p""", """1""")) // Here's an example of a GET request with a query
    .pause(670 milliseconds)
    .exec(http("request_6")
      .get("/computers")
      .queryParam("""p""", """2"""))
    .pause(629 milliseconds)
    .exec(http("request_7")
      .get("/computers")
      .queryParam("""p""", """3"""))
    .pause(734 milliseconds)
    .exec(http("request_8")
      .get("/computers")
      .queryParam("""p""", """4"""))
    .pause(5)
      .exec(http("request_9")
      .get("/computers/new"))
    .pause(1)
    .exec(http("request_10") // Here's an example of a POST request
      .post("/computers")
      .headers(headers_10)
      .param("""name""", """Beautiful Computer""")
      .param("""introduced""", """2012-05-30""")
      .param("""discontinued""", """""")
      .param("""company""", """37"""))

  setUp(scn.inject(atOnceUsers(1)).protocols(httpConf))
}
