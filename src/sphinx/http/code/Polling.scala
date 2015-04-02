import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class Polling {

  //#pollerName
  polling.pollerName("myCustomName")
  //#pollerName

  //#pollerStart
  exec(
    polling
      .every(10 seconds)
      .exec(http("name").get("url"))
  )
  //#pollerStart

  //#pollerStop
  exec(polling.stop)
  //#pollerStop
}
