****
HTTP
****

Gatling HTTP protocol is a dedicated DSL

Preamble: Tuning
================



Building a request
==================

Common parameters
-----------------

queryParam(key: Expression[String], value: Expression[Any])
multivaluedQueryParam(key: Expression[String], values: Expression[Seq[Any]])
queryParamsSequence(seq: Expression[Seq[(String, Any)]])
queryParamsMap(map: Expression[Map[String, Any]])
queryParam(param: HttpParam)
header(name: String, value: Expression[String]):
headers(newHeaders: Map[String, String])
asJSON
asXML
basicAuth(username: Expression[String], password: Expression[String])
digestAuth(username: Expression[String], password: Expression[String])
authRealm(realm: Expression[Realm])
virtualHost(virtualHost: Expression[String])
address(address: InetAddress)
useRawUrl
proxy(httpProxy: Proxy)

Regular HTTP request
--------------------

check(checks: HttpCheck* )
ignoreDefaultChecks
extraInfoExtractor(f: ExtraInfoExtractor)
transformResponse(responseTransformer: ResponseTransformer)
maxRedirects(max: Int)
body(bd: Body)
processRequestBody(processor: Body => Body)
bodyPart(bodyPart: BodyPart)
resources(res: AbstractHttpRequestBuilder[_]*)


POST HTTP request
-----------------

asMultipartForm
param(key: Expression[String], value: Expression[Any])
multivaluedParam(key: Expression[String], values: Expression[Seq[Any]])
paramsSequence(seq: Expression[Seq[(String, Any)]])
paramsMap(map: Expression[Map[String, Any]])
param(param: HttpParam)
formUpload(name: Expression[String], filePath: Expression[String])


WebSockets
----------










Configuring HTTP Protocol
=========================


Core parameters
---------------

baseURL(url: String)
baseURLs(urls: String*) / baseURLs(baseUrls: List[String])
warmUp(url: String)
disableWarmUp


Engine parameters
-----------------

disableClientSharing
shareConnections
virtualHost(virtualHost: Expression[String])
localAddress(localAddress: InetAddress)
maxConnectionsPerHostLikeFirefoxOld
maxConnectionsPerHostLikeFirefox
maxConnectionsPerHostLikeOperaOld
maxConnectionsPerHostLikeOpera
maxConnectionsPerHostLikeSafariOld
maxConnectionsPerHostLikeSafari
maxConnectionsPerHostLikeIE7
maxConnectionsPerHostLikeIE8
maxConnectionsPerHostLikeIE10
maxConnectionsPerHostLikeChrome
maxConnectionsPerHost(max: Int)

Request building parameters
------------------


disableAutoReferer
disableCaching
baseHeaders(headers: Map[String, String])
acceptHeader(value: Expression[String])
acceptCharsetHeader(value: Expression[String])
acceptEncodingHeader(value: Expression[String])
acceptLanguageHeader(value: Expression[String])
authorizationHeader(value: Expression[String])
connection(value: Expression[String])
doNotTrackHeader(value: Expression[String])
userAgentHeader(value: Expression[String])
basicAuth(username: Expression[String], password: Expression[String])
digestAuth(username: Expression[String], password: Expression[String])
authRealm(realm: Expression[Realm])

Response handling parameters
----------------------------

disableFollowRedirect
maxRedirects(max: Int)
disableResponseChunksDiscarding
extraInfoExtractor(f: ExtraInfoExtractor)
transformResponse(responseTransformer: ResponseTransformer)
check(checks: HttpCheck*)
fetchHtmlResources
fetchHtmlResources(white: WhiteList)
fetchHtmlResources(white: WhiteList, black: BlackList)
fetchHtmlResources(black: BlackList, white: WhiteList = WhiteList(Nil))
def fetchHtmlResources(filters: Option[Filters])

WebSockets parameters
---------------------


wsBaseURL(baseUrl: String)
wsBaseURLs(baseUrl1: String, baseUrl2: String, baseUrls: String*)
wsBaseURLs(baseUrls: List[String])
wsReconnect
wsMaxReconnects(max: Int)

Proxy parameters
----------------

noProxyFor(hosts: String*)
proxy(httpProxy: Proxy)