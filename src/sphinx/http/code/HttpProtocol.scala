import io.gatling.core.Predef._
import io.gatling.http.Predef._

class HttpProtocol extends Simulation {

  {
    //#bootstrapping
    val httpConf = http.baseURL("http://my.website.tld")

    val scn = scenario("myScenario") // etc...

    setUp(scn.inject(atOnceUsers(1)).protocols(httpConf))
    //#bootstrapping
  }

  {
    //#baseUrl
    val httpConf = http.baseURL("http://my.website.tld")

    val scn = scenario("My Scenario")
      .exec(
        http("My Request")
          .get("/my_path")) // Will actually make a request on "http://my.website.tld/my_path"
      .exec(
        http("My Other Request")
          .get("http://other.website.tld")) // Will make a request on "http://other.website.tld"

    setUp(scn.inject(atOnceUsers(1)).protocols(httpConf))
    //#baseUrl
  }

  {
    //#baseUrls
    val httpConf = http.baseURLs("http://my1.website.tld", "http://my2.website.tld", "http://my3.website.tld")
    //#baseUrls
  }

  {
    //#warmUp
    // override warm up URL to http://www.google.com
    val httpConf = http.warmUp("http://www.google.com")
    // disable warm up
    val httpConfNoWarmUp = http.disableWarmUp
    //#warmUp
  }

  {
    //#maxConnectionsPerHost
    // 10 connections per host.
    val httpConfMax10Connections = http.maxConnectionsPerHost(10)
    // Firefox max connections per host preset.
    val httpConfMaxConnectionsLikeFirefox = http.maxConnectionsPerHostLikeFirefox
    //#maxConnectionsPerHost
  }

  {
    val httpConf = http
      //#silentURI
      .silentURI("https://myCDN/.*")
      //#silentURI
      //#headers
      .header("foo", "bar")
      .headers(Map("foo" -> "bar", "baz" -> "qix"))
      //#headers
  }

  {
    //#proxy
    val httpConf = http
      .proxy(
        Proxy("myProxyHost", 8080)
          .httpsPort(8143)
          .credentials("myUsername", "myPassword")
      )
    //#proxy

  }

  {
    //#noProxyFor
    val httpConf = http
      .proxy(Proxy("myProxyHost", 8080))
      .noProxyFor("www.github.com", "www.akka.io")
    //#noProxyFor
  }
}
