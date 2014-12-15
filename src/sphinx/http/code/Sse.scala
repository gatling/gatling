import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class Sse {

  //#sseName
  sse("SSE Operation").sseName("myCustomName")
  //#sseName

  //#sseOpen
  exec(sse("Get SSE").open("/stocks/prices"))
  //#sseOpen

  //#sseClose
  exec(sse("Close SSE").close())
  //#sseClose

  val myCheck = wsAwait.within(10).until(1).regex(""""event":"snapshot(.*)"""")

  //#check-from-message
  exec(sse("Get SSE").open("/stocks/prices").check(myCheck))
  //#check-from-message

  //#check-from-flow
  exec(sse("Set Check").check(myCheck))
  //#check-from-flow

  //#cancel-check
  exec(sse("Cancel Check").cancelCheck)
  //#cancel-check

  //#build-check
  exec(sse("sse").open("/stocks/prices")
    .check(wsAwait.within(10).until(1).regex(""""event":"snapshot(.*)"""")))

  exec(sse("sse").open("/stocks/prices")
    .check(wsListen.within(30 seconds).expect(1)))
  //#build-check

  //#stock-market-sample
  val httpConf = http
    .baseURL("http://localhost:8080/app")

  val scn = scenario("Server Sent Event")
    .exec(
      sse("Stocks").open("/stocks/prices")
      .check(wsAwait.within(10).until(1).regex(""""event":"snapshot(.*)"""")))
    .pause(15)
    .exec(sse("Close SSE").close())
  //#stock-market-sample
}
