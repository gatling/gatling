import io.gatling.core.Predef._

class Assertions extends Simulation {

  val scn = scenario("foo").inject(atOnceUsers(1))

  //#setUp
  setUp(scn).assertions(
    global.responseTime.max.lessThan(50),
    global.successfulRequests.percent.greaterThan(95)
  )
  //#setUp

  //#details
  details("Search" / "Index")
  //#details

  //#examples
  // Assert that the max response time of all requests is less than 100 ms
  setUp(scn).assertions(global.responseTime.max.lessThan(100))

  // Assert that every request has no more than 5% of failing requests
  setUp(scn).assertions(forAll.failedRequests.percent.lessThan(5))

  // Assert that the percentage of failed requests named "Index" in the group "Search"
  // is exactly 0 %
  setUp(scn).assertions(details("Search" / "Index").failedRequests.percent.is(0))

  // Assert that the rate of requests per seconds for the group "Search"
  setUp(scn).assertions(details("Search").requestsPerSec.between(100, 1000))
  //#examples
}
