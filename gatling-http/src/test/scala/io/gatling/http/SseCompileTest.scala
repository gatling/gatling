package io.gatling.http

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
 * @author ctranxuan
 */
class SseCompileTest extends Simulation {

  val httpConf = http
                 .baseURL("http://localhost:8080/app")
                 .header("Accept", "text/event-stream")
                 .doNotTrackHeader("1")

  val scn = scenario(this.getClass.getSimpleName)
            .exec(sse("sse")
                  .get("/stocks/prices")
                  .check(wsAwait.within(10).until(1).regex("""event: snapshot(.*)""").count.is(1)))
            .pause(15)
            .exec(sse("close").close())

  setUp(scn.inject(rampUsers(100) over 10)).protocols(httpConf)
}