package io.gatling.http.request.builder

import java.net.URI
import io.gatling.core.session.Expression

/**
 * @param requestName the name of the request
 */
class Http(requestName: Expression[String]) {

	def get(url: Expression[String]) = httpRequest("GET", Left(url))
	def get(uri: URI) = httpRequest("GET", Right(uri))
	def put(url: Expression[String]) = httpRequest("PUT", Left(url))
	def patch(url: Expression[String]) = httpRequest("PATCH", Left(url))
	def head(url: Expression[String]) = httpRequest("HEAD", Left(url))
	def delete(url: Expression[String]) = httpRequest("DELETE", Left(url))
	def options(url: Expression[String]) = httpRequest("OPTIONS", Left(url))
	def httpRequest(method: String, urlOrURI: Either[Expression[String], URI]) = new HttpRequestBuilder(CommonAttributes(requestName, method, urlOrURI), HttpAttributes())

	def post(url: Expression[String]) = httpRequestWithParams("POST", Left(url))
	def httpRequestWithParams(method: String, urlOrURI: Either[Expression[String], URI]) = new HttpRequestWithParamsBuilder(CommonAttributes(requestName, method, urlOrURI), HttpAttributes(), Nil)
}
