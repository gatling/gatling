/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.Proxy;
import static io.gatling.javaapi.http.HttpDsl.http;

class HttpProtocolSampleJava extends Simulation {

  {
//#bootstrapping
HttpProtocolBuilder httpProtocol = http.baseUrl("https://gatling.io");

ScenarioBuilder scn = scenario("Scenario"); // etc...

setUp(scn.injectOpen(atOnceUsers(1)).protocols(httpProtocol));
//#bootstrapping
    }

  {
//#baseUrl
HttpProtocolBuilder httpProtocol = http.baseUrl("https://gatling.io");

ScenarioBuilder scn = scenario("Scenario")
  // will make a request to "https://gatling.io/docs/"
  .exec(
    http("Relative").get("/doc/")
  )
  // will make a request to "https://github.com/gatling/gatling"
  .exec(
    http("Absolute").get("https://github.com/gatling/gatling")
  );

setUp(scn.injectOpen(atOnceUsers(1)).protocols(httpProtocol));
//#baseUrl
  }

  static {
//#baseUrls
http.baseUrls(
  "https://gatling.io",
  "https://github.com"
);
//#baseUrls

//#warmUp
// change the warm up URL to https://www.google.com
http.warmUp("https://www.google.com");
// disable warm up
http.disableWarmUp();
//#warmUp

//#maxConnectionsPerHost
http.maxConnectionsPerHost(10);
//#maxConnectionsPerHost

//#shareConnections
http.shareConnections();
//#shareConnections

//#enableHttp2
http.enableHttp2();
//#enableHttp2

//#http2PriorKnowledge
Map<String, Boolean> priorKnowledge = new HashMap<>();
priorKnowledge.put("www.google.com", true);
priorKnowledge.put("gatling.io", false);

http
  .enableHttp2()
  .http2PriorKnowledge(priorKnowledge);
//#http2PriorKnowledge

//#dns-async
// use hosts' configured DNS servers on Linux and MacOS
// use Google's DNS servers on Windows
http.asyncNameResolution();

// force DNS servers
http.asyncNameResolution("8.8.8.8");

// instead of having a global DNS resolver
// have each virtual user to have its own (hence own cache, own UDP requests)
// only effective with asyncNameResolution
http
  .asyncNameResolution()
  .perUserNameResolution();
//#dns-async

//#hostNameAliases
http
  .hostNameAliases(Collections.singletonMap("gatling.io", Arrays.asList("192.168.0.1", "192.168.0.2")));
//#hostNameAliases

//#virtualHost
// with a static value
http.virtualHost("virtualHost");
// with a Gatling EL string
http.virtualHost("#{virtualHost}");
// with a function
http.virtualHost(session -> session.getString("virtualHost"));
//#virtualHost

//#localAddress
http.localAddress("localAddress");

http.localAddresses("localAddress1", "localAddress2");

// automatically discover all bindable local addresses
http.useAllLocalAddresses();

// automatically discover all bindable local addresses
// matching one of the Java Regex patterns
http.useAllLocalAddressesMatching("pattern1", "pattern2");
//#localAddress

//#perUserKeyManagerFactory
http.perUserKeyManagerFactory(userId -> (javax.net.ssl.KeyManagerFactory) null);
//#perUserKeyManagerFactory

//#disableAutoReferer
http.disableAutoReferer();
//#disableAutoReferer

//#disableCaching
http.disableCaching();
//#disableCaching

//#disableUrlEncoding
http.disableUrlEncoding();
//#disableUrlEncoding

//#silentUri
// make all requests whose url matches the provided Java Regex pattern silent
http.silentUri("https://myCDN/.*");
// make all resource requests silent
http.silentResources();
//#silentUri

//#headers
http
  // with a static header value
  .header("foo", "bar")
  // with a Gatling EL string header value
  .header("foo", "#{headerValue}")
  // with a function value
  .header("foo", session -> session.getString("headerValue"))
  .headers(Collections.singletonMap("foo", "bar"));
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
  .upgradeInsecureRequestsHeader("value");
//#headers-built-ins

//#sign
http.sign(request -> {
  // import org.apache.commons.codec.digest.DigestUtils
  String md5 = DigestUtils.md5Hex(request.getBody().getBytes());
  request.getHeaders().add("X-MD5", md5);
  return request;
});

// or with access to the Session
http.sign((request, session) -> request);
//#sign

//#sign-oauth1
// with static values
http.signWithOAuth1(
  "consumerKey",
 "clientSharedSecret",
  "token",
  "tokenSecret"
);
// with functions
http.signWithOAuth1(
  session -> session.getString("consumerKey"),
  session -> session.getString("clientSharedSecret"),
  session -> session.getString("token"),
  session -> session.getString("tokenSecret")
);
// pass signature as form params or query params instead of an Authorization header
http.signWithOAuth1(
  "consumerKey",
  "clientSharedSecret",
  "token",
  "tokenSecret",
  false
);
//#sign-oauth1

//#authorization
// with static values
http.basicAuth("username", "password");
// with Gatling El strings
http.basicAuth("#{username}", "#{password}");
// with functions
http.basicAuth(session -> session.getString("username"), session -> session.getString("password"));

// with static values
http.digestAuth("username", "password");
// with Gatling El strings
http.digestAuth("#{username}", "#{password}");
// with functions
http.digestAuth(session -> session.getString("username"), session -> session.getString("password"));
//#authorization

//#disableFollowRedirect
http.disableFollowRedirect();
//#disableFollowRedirect

//#redirectNamingStrategy
http.redirectNamingStrategy(
  (uri, originalRequestName, redirectCount) -> "redirectedRequestName"
);
//#redirectNamingStrategy

//#transformResponse
http.transformResponse((response, session) -> response);
//#transformResponse

//#inferHtmlResources
// fetch only resources matching one of the patterns in the allow list
http.inferHtmlResources(AllowList("pattern1", "pattern2"));
// fetch all resources except those matching one of the patterns in the deny list
http.inferHtmlResources(DenyList("pattern1", "pattern2"));
// fetch only resources matching one of the patterns in the allow list
// but not matching one of the patterns in the deny list
http.inferHtmlResources(AllowList("pattern1", "pattern2"), DenyList("pattern3", "pattern4"));
//#inferHtmlResources

//#nameInferredHtmlResources
// (default): name requests after the resource's url tail (after last `/`)
http.nameInferredHtmlResourcesAfterUrlTail();
// name requests after the resource's path
http.nameInferredHtmlResourcesAfterPath();
// name requests after the resource's absolute url
http.nameInferredHtmlResourcesAfterAbsoluteUrl();
// name requests after the resource's relative url
http.nameInferredHtmlResourcesAfterRelativeUrl();
// name requests after the resource's last path element
http.nameInferredHtmlResourcesAfterLastPathElement();
// name requests with a custom strategy
http.nameInferredHtmlResources(uri -> "name");
//#nameInferredHtmlResources

//#proxy
http.proxy(
  Proxy("myHttpProxyHost", 8080)
    .httpsPort(8143)
    .credentials("myUsername", "myPassword")
);

http.proxy(
  Proxy("mySocks4ProxyHost", 8080)
    .socks4()
);

http.proxy(
  Proxy("mySocks5ProxyHost", 8080)
    .httpsPort(8143)
    .socks5()
);
//#proxy

//#noProxyFor
http
  .proxy(Proxy("myProxyHost", 8080))
  .noProxyFor("www.github.com", "gatling.io");
//#noProxyFor
  }
}
