/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
import org.apache.commons.codec.digest.DigestUtils

class HttpProtocolSampleScala extends Simulation {
  {
//#bootstrapping
val httpProtocol = http.baseUrl("https://gatling.io")

val scn = scenario("Scenario") // etc...

setUp(scn.inject(atOnceUsers(1)).protocols(httpProtocol))
//#bootstrapping
  }

  {
//#baseUrl
val httpProtocol = http.baseUrl("https://gatling.io")

val scn = scenario("Scenario")
  // will make a request to "https://gatling.io/docs/"
  .exec(
    http("Relative").get("/docs/")
  )
  // will make a request to "https://github.com/gatling/gatling"
  .exec(
    http("Absolute").get("https://github.com/gatling/gatling")
  )

setUp(scn.inject(atOnceUsers(1)).protocols(httpProtocol))
//#baseUrl
  }

//#baseUrls
http.baseUrls(
  "https://gatling.io",
  "https://github.com"
)
//#baseUrls

//#warmUp
// change the warm up URL to https://www.google.com
http.warmUp("https://www.google.com")
// disable warm up
http.disableWarmUp
//#warmUp

//#maxConnectionsPerHost
http.maxConnectionsPerHost(10)
//#maxConnectionsPerHost

//#shareConnections
http.shareConnections
//#shareConnections

//#enableHttp2
http.enableHttp2
//#enableHttp2

//#http2PriorKnowledge
http
  .enableHttp2
  .http2PriorKnowledge(
    Map(
      "www.google.com" -> true,
      "gatling.io" -> false
    )
  )
//#http2PriorKnowledge

//#dns-async
// use hosts' configured DNS servers on Linux and MacOS
// use Google's DNS servers on Windows
http.asyncNameResolution()

// force DNS servers
http.asyncNameResolution("8.8.8.8")

// instead of having a global DNS resolver
// have each virtual user to have its own (hence own cache, own UDP requests)
// only effective with asyncNameResolution
http
  .asyncNameResolution()
  .perUserNameResolution
//#dns-async

//#hostNameAliases
http
  .hostNameAliases(Map("gatling.io" -> List("192.168.0.1", "192.168.0.2")))
//#hostNameAliases

//#localAddress
http.localAddress("localAddress")

http.localAddresses("localAddress1", "localAddress2")

// automatically discover all bindable local addresses
http.useAllLocalAddresses

// automatically discover all bindable local addresses
// matching one of the Java Regex patterns
http.useAllLocalAddressesMatching("pattern1", "pattern2")
//#localAddress

//#perUserKeyManagerFactory
http.perUserKeyManagerFactory(userId => null.asInstanceOf[javax.net.ssl.KeyManagerFactory])
//#perUserKeyManagerFactory

//#disableAutoReferer
http.disableAutoReferer
//#disableAutoReferer

//#disableCaching
http.disableCaching
//#disableCaching

//#disableUrlEncoding
http.disableUrlEncoding
//#disableUrlEncoding

//#silentUri
// make all requests whose url matches the provided pattern silent
http.silentUri("https://myCDN/.*")
// make all resource requests silent
http.silentResources
//#silentUri

//#headers
http
  // with a static header value
  .header("foo", "bar")
  // with a Gatling EL string header value
  .header("foo", "#{headerValue}")
  // with a function value
  .header("foo", session => session("headerValue").as[String])
  .headers(Map("foo" -> "bar", "baz" -> "qix"))
//#headers

//#headers-built-ins
// all those also accept a Gatling EL string or a function parameter
http
  .acceptHeader("value")
  .acceptCharsetHeader("value")
  .acceptEncodingHeader("value")
  .acceptLanguageHeader("value")
  .authorizationHeader("value")
  .connectionHeader("value")
  .contentTypeHeader("value")
  .doNotTrackHeader("value")
  .originHeader("value")
  .userAgentHeader("value")
  .upgradeInsecureRequestsHeader("value")
//#headers-built-ins

//#sign
http.sign { (request, session) =>
  // import org.apache.commons.codec.digest.DigestUtils
  val md5 = DigestUtils.md5Hex(request.getBody.getBytes)
  request.getHeaders.add("X-MD5", md5)
  request
}
//#sign

//#sign-oauth1
// parameters can also be Gatling EL strings or functions
http.signWithOAuth1(
  "consumerKey",
  "clientSharedSecret",
  "token",
  "tokenSecret"
)
// pass signature as form params or query params instead of an Authorization header
http.signWithOAuth1(
  "consumerKey",
  "clientSharedSecret",
  "token",
  "tokenSecret",
  false
)
//#sign-oauth1

//#authorization
// with static values
http.basicAuth("username", "password")
// with Gatling El strings
http.basicAuth("#{username}", "#{password}")
// with functions
http.basicAuth(session => session("username").as[String], session => session("password").as[String])

// with static values
http.digestAuth("username", "password")
// with Gatling El strings
http.digestAuth("#{username}", "#{password}")
// with functions
http.digestAuth(session => session("username").as[String], session => session("password").as[String])
//#authorization

//#disableFollowRedirect
http.disableFollowRedirect
//#disableFollowRedirect

//#redirectNamingStrategy
http.redirectNamingStrategy(
  (uri, originalRequestName, redirectCount) => "redirectedRequestName"
)
//#redirectNamingStrategy

//#transformResponse
http.transformResponse((response, session) => response)
//#transformResponse

//#inferHtmlResources
// fetch only resources matching one of the patterns in the allow list
http.inferHtmlResources(AllowList("pattern1", "pattern2"))
// fetch all resources except those matching one of the patterns in the deny list
http.inferHtmlResources(DenyList("pattern1", "pattern2"))
// fetch only resources matching one of the patterns in the allow list
// but not matching one of the patterns in the deny list
http.inferHtmlResources(AllowList("pattern1", "pattern2"), DenyList("pattern3", "pattern4"))
//#inferHtmlResources

//#nameInferredHtmlResources
// (default): name requests after the resource's url tail (after last `/`)
http.nameInferredHtmlResourcesAfterUrlTail
// name requests after the resource's path
http.nameInferredHtmlResourcesAfterPath
// name requests after the resource's absolute url
http.nameInferredHtmlResourcesAfterAbsoluteUrl
// name requests after the resource's relative url
http.nameInferredHtmlResourcesAfterRelativeUrl
// name requests after the resource's last path element
http.nameInferredHtmlResourcesAfterLastPathElement
// name requests with a custom strategy
http.nameInferredHtmlResources(uri => "name")
//#nameInferredHtmlResources

//#proxy
// clear HTTP proxy
http.proxy(
  Proxy("myHttpProxyHost", 8080)
    .credentials("myUsername", "myPassword")
)

// HTTPS proxy
http.proxy(
  Proxy("myHttpProxyHost", 8080)
    .https
)

// SOCKS4 proxy
http.proxy(
  Proxy("mySocks4ProxyHost", 8080)
    .socks4
)

// SOCKS5 proxy
http.proxy(
  Proxy("mySocks5ProxyHost", 8080)
    .socks5
)
//#proxy

//#noProxyFor
http
  .proxy(Proxy("myProxyHost", 8080))
  .noProxyFor("www.github.com", "gatling.io")
//#noProxyFor

//#proxyProtocolSource
http
  // use a Gatling EL to pass the IPV4 local address
  // to be used to generate a PROXY protocol header
  // when the connection uses IPv4
  .proxyProtocolSourceIpV4Address("#{myIpV4AddressString}")
  // use a function instead,
  // but it will be resolved on each request execution
  .proxyProtocolSourceIpV4Address(session => "199.60.103.60")
  // use a Gatling EL to pass the IPV6 local address
  // to be used to generate a PROXY protocol header
  // when the connection uses IPv6
  .proxyProtocolSourceIpV6Address("#{myIpV6AddressString}")
  // use a function instead,
  // but it will be resolved on each request execution
  .proxyProtocolSourceIpV6Address(session => "2a00:1450:4007:810::200e")
//#proxyProtocolSource
}
