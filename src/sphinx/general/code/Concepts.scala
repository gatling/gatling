import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class Concepts extends Simulation {

  //#simple-scenario
  scenario("Standard User")
    .exec(http("Access Github").get("https://github.com"))
    .pause(2, 3)
    .exec(http("Search for 'gatling'").get("https://github.com/search?q=gatling"))
    .pause(2)
  //#simple-scenario

  //#example-definition
  val stdUser = scenario("Standard User") // etc..
  val admUser = scenario("Admin User") // etc..
  val advUser = scenario("Advanced User") // etc..

  setUp(
    stdUser.inject(atOnceUsers(2000)),
    admUser.inject(nothingFor(60 seconds), rampUsers(5) over (400 seconds)),
    advUser.inject(rampUsers(500) over (200 seconds))
  )
  //#example-definition
}
