package io.gatling.http.funspec

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.funspec.GatlingHttpFunSpecCompileTest._

class GatlingHttpFunSpecCompileTest extends GatlingHttpFunSpec {

  val baseURL = "http://example.com"
  override def httpConf = super.httpConf.header("MyHeader", "MyValue")

  spec {
    http("Index test")
      .get("/index.html")
      .check(h1 exists)
  }

}

object GatlingHttpFunSpecCompileTest {

  def h1 = css("h1")

}
