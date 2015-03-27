import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.funspec.GatlingHttpFunSpec
import GatlingFunSpecExample._

//#example-test
class GatlingFunSpecExample extends GatlingHttpFunSpec { // (1)

  val baseURL = "http://example.com" // (2)
  override def httpConf = super.httpConf.header("MyHeader", "MyValue") // (3)

  spec { // (4)
    http("Example index.html test") // (5)
      .get("/index.html") // (6)
      .check(pageHeader.exists) // (7)
  }

}

object GatlingFunSpecExample {
  def pageHeader = css("h1") // (8)
}
//#example-test
