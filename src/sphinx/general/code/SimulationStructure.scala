import io.gatling.core.Predef._
import io.gatling.http.Predef._

class SimulationStructure extends Simulation {

  val httpConf = http

  //#headers
  val headers_10 = Map("Content-Type" -> """application/x-www-form-urlencoded""")
  //#headers

  //#scenario-definition
  val scn = scenario("ScenarioName") // etc...
  //#scenario-definition

  //#http-request-sample
  // Here's an example of a POST request
  http("request_10")
    .post("/computers")
    .headers(headers_10)
    .formParam("name", "Beautiful Computer")
    .formParam("introduced", "2012-05-30")
    .formParam("discontinued", "")
    .formParam("company", "37")
  //#http-request-sample

  //#setUp
  setUp(
    scn.inject(atOnceUsers(1)) // (1)
      .protocols(httpConf) // (2)
  )
  //#setUp

  //#hooks
  before {
    println("Simulation is about to start!")
  }

  after {
    println("Simulation is finished!")
  }
  //#hooks
}
