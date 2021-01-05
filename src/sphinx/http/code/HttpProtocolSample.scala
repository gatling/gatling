/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class HttpProtocolSample extends Simulation {

  {
    //#bootstrapping
    val httpProtocol = http.baseUrl("http://my.website.tld")

    val scn = scenario("myScenario") // etc...

    setUp(scn.inject(atOnceUsers(1)).protocols(httpProtocol))
    //#bootstrapping
  }

  {
    //#baseUrl
    val httpProtocol = http.baseUrl("http://my.website.tld")

    val scn = scenario("My Scenario")
      .exec(
        http("My Request")
          .get("/my_path")
      ) // will make a request to "http://my.website.tld/my_path"
      .exec(
        http("My Other Request")
          .get("http://other.website.tld")
      ) // will make a request to "http://other.website.tld"

    setUp(scn.inject(atOnceUsers(1)).protocols(httpProtocol))
    //#baseUrl
  }

  {
    //#baseUrls
    val httpProtocol = http.baseUrls("http://my1.website.tld", "http://my2.website.tld", "http://my3.website.tld")
    //#baseUrls
  }

  {
    //#warmUp
    // override warm up URL to http://www.google.com
    val httpProtocol = http.warmUp("http://www.google.com")
    // disable warm up
    val httpProtocolNoWarmUp = http.disableWarmUp
    //#warmUp
  }

  {
    //#maxConnectionsPerHost
    // 10 connections per host.
    val httpProtocolMax10Connections = http.maxConnectionsPerHost(10)
    // Firefox max connections per host preset.
    val httpProtocolMaxConnectionsLikeFirefox = http.maxConnectionsPerHostLikeFirefox
    //#maxConnectionsPerHost
  }

  {
    val httpProtocol = http
    //#silentUri
      .silentUri("https://myCDN/.*")
      //#silentUri
      //#headers
      .header("foo", "bar")
      .headers(Map("foo" -> "bar", "baz" -> "qix"))
    //#headers
  }

  {
    //#proxy
    val httpProtocol = http
      .proxy(
        Proxy("myHttpProxyHost", 8080)
          .httpsPort(8143)
          .credentials("myUsername", "myPassword")
      )
      .proxy(
        Proxy("mySocks4ProxyHost", 8080).socks4
      )
      .proxy(
        Proxy("mySocks5ProxyHost", 8080)
          .httpsPort(8143)
          .socks5
      )
    //#proxy

  }

  {
    //#noProxyFor
    val httpProtocol = http
      .proxy(Proxy("myProxyHost", 8080))
      .noProxyFor("www.github.com", "www.akka.io")
    //#noProxyFor
  }

  {
    //#enableHttp2
    val httpProtocol = http.enableHttp2
    //#enableHttp2
  }

  {
    //#http2PriorKnowledge
    val httpProtocol = http.enableHttp2
      .http2PriorKnowledge(Map("www.google.com" -> true, "gatling.io" -> false))
    //#http2PriorKnowledge
  }

  {
    //#hostNameAliases
    val httpProtocol = http
      .hostNameAliases(Map("gatling.io" -> List("192.168.0.1", "192.168.0.2")))
    //#hostNameAliases
  }
}
