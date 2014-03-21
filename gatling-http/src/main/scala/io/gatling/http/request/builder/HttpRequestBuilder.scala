/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request.builder

import com.ning.http.client.Request

import io.gatling.core.session.Expression
import io.gatling.http.action.HttpRequestActionBuilder
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckOrder.Status
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.{ Body, BodyPart, ExtraInfoExtractor, HttpRequest }
import io.gatling.http.response.ResponseTransformer

case class HttpAttributes(
	checks: List[HttpCheck] = Nil,
	ignoreDefaultChecks: Boolean = false,
	silent: Boolean = false,
	responseTransformer: Option[ResponseTransformer] = None,
	explicitResources: Seq[AbstractHttpRequestBuilder[_]] = Nil,
	body: Option[Body] = None,
	bodyParts: List[BodyPart] = Nil,
	extraInfoExtractor: Option[ExtraInfoExtractor] = None)

object AbstractHttpRequestBuilder {

	implicit def toActionBuilder(requestBuilder: AbstractHttpRequestBuilder[_]) = new HttpRequestActionBuilder(requestBuilder)
}

/**
 * This class serves as model for all HttpRequestBuilders
 *
 * @param httpAttributes the base HTTP attributes
 */
abstract class AbstractHttpRequestBuilder[B <: AbstractHttpRequestBuilder[B]](commonAttributes: CommonAttributes, val httpAttributes: HttpAttributes)
	extends RequestBuilder[B](commonAttributes) {

	/**
	 * Method overridden in children to create a new instance of the correct type
	 */
	private[http] def newInstance(httpAttributes: HttpAttributes): B

	/**
	 * Stops defining the request and adds checks on the response
	 *
	 * @param checks the checks that will be performed on the response
	 */
	def check(checks: HttpCheck*): B = newInstance(httpAttributes.copy(checks = httpAttributes.checks ::: checks.toList))

	/**
	 * Ignore the default checks configured on HttpProtocol
	 */
	def ignoreDefaultChecks: B = newInstance(httpAttributes.copy(ignoreDefaultChecks = true))

	def silent: B = newInstance(httpAttributes.copy(silent = true))

	def extraInfoExtractor(f: ExtraInfoExtractor): B = newInstance(httpAttributes.copy(extraInfoExtractor = Some(f)))

	/**
	 * @param responseTransformer transforms the response before it's handled to the checks pipeline
	 */
	def transformResponse(responseTransformer: ResponseTransformer): B = newInstance(httpAttributes.copy(responseTransformer = Some(responseTransformer)))

	def body(bd: Body): B = newInstance(httpAttributes.copy(body = Some(bd)))

	def processRequestBody(processor: Body => Body): B = newInstance(httpAttributes.copy(body = httpAttributes.body.map(processor)))

	def bodyPart(bodyPart: BodyPart): B = newInstance(httpAttributes.copy(bodyParts = bodyPart :: httpAttributes.bodyParts))

	def resources(res: AbstractHttpRequestBuilder[_]*): B = newInstance(httpAttributes.copy(explicitResources = res))

	def ahcRequest(protocol: HttpProtocol): Expression[Request]

	/**
	 * This method builds the request that will be sent
	 *
	 * @param protocol the protocol of the current scenario
	 * @param throttled if throttling is enabled
	 */
	def build(protocol: HttpProtocol, throttled: Boolean): HttpRequest = {

		val checks =
			if (httpAttributes.ignoreDefaultChecks)
				httpAttributes.checks
			else
				protocol.responsePart.checks ::: httpAttributes.checks

		val resolvedChecks = (checks.find(_.order == Status) match {
			case None => HttpRequestActionBuilder.defaultHttpCheck :: checks
			case _ => checks
		}).sorted

		val resolvedResponseTransformer = httpAttributes.responseTransformer.orElse(protocol.responsePart.responseTransformer)

		val resolvedResources = httpAttributes.explicitResources.filter(_.commonAttributes.method == "GET").map(_.build(protocol, throttled))

		val resolvedExtraInfoExtractor = httpAttributes.extraInfoExtractor.orElse(protocol.responsePart.extraInfoExtractor)

		HttpRequest(
			commonAttributes.requestName,
			ahcRequest(protocol),
			resolvedChecks,
			resolvedResponseTransformer,
			resolvedExtraInfoExtractor,
			protocol.responsePart.maxRedirects,
			throttled,
			httpAttributes.silent,
			protocol,
			resolvedResources)
	}
}

class HttpRequestBuilder(commonAttributes: CommonAttributes, httpAttributes: HttpAttributes) extends AbstractHttpRequestBuilder[HttpRequestBuilder](commonAttributes, httpAttributes) {

	private[http] def newInstance(commonAttributes: CommonAttributes) = new HttpRequestBuilder(commonAttributes, httpAttributes)
	private[http] def newInstance(httpAttributes: HttpAttributes) = new HttpRequestBuilder(commonAttributes, httpAttributes)
	def ahcRequest(protocol: HttpProtocol) = new HttpRequestExpressionBuilder(commonAttributes, httpAttributes, protocol).build
}
