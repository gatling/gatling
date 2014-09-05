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
import io.gatling.http.check.HttpCheckScope.Status
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request._
import io.gatling.http.response.Response

case class HttpAttributes(
  checks: List[HttpCheck] = Nil,
  ignoreDefaultChecks: Boolean = false,
  silent: Option[Boolean] = None,
  followRedirect: Boolean = true,
  discardResponseChunks: Boolean = true,
  responseTransformer: Option[PartialFunction[Response, Response]] = None,
  explicitResources: List[AbstractHttpRequestBuilder[_]] = Nil,
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

  def silent: B = newInstance(httpAttributes.copy(silent = Some(true)))

  def notSilent: B = newInstance(httpAttributes.copy(silent = Some(false)))

  def disableFollowRedirect: B = newInstance(httpAttributes.copy(followRedirect = false))

  def extraInfoExtractor(f: ExtraInfoExtractor): B = newInstance(httpAttributes.copy(extraInfoExtractor = Some(f)))

  /**
   * @param responseTransformer transforms the response before it's handled to the checks pipeline
   */
  def transformResponse(responseTransformer: PartialFunction[Response, Response]): B = newInstance(httpAttributes.copy(responseTransformer = Some(responseTransformer)))

  def body(bd: Body): B = newInstance(httpAttributes.copy(body = Some(bd)))

  def processRequestBody(processor: Body => Body): B = newInstance(httpAttributes.copy(body = httpAttributes.body.map(processor)))

  def bodyPart(bodyPart: BodyPart): B = newInstance(httpAttributes.copy(bodyParts = bodyPart :: httpAttributes.bodyParts))

  def resources(res: AbstractHttpRequestBuilder[_]*): B = newInstance(httpAttributes.copy(explicitResources = res.toList))

  def disableResponseChunksDiscarding = newInstance(httpAttributes.copy(discardResponseChunks = false))

  def request(protocol: HttpProtocol): Expression[Request]

  /**
   * This method builds the request that will be sent
   *
   * @param protocol the protocol of the current scenario
   * @param throttled if throttling is enabled
   */
  def build(protocol: HttpProtocol, throttled: Boolean): HttpRequestDef = {

    val checks =
      if (httpAttributes.ignoreDefaultChecks)
        httpAttributes.checks
      else
        protocol.responsePart.checks ::: httpAttributes.checks

    val resolvedChecks = checks.find(_.scope == Status) match {
      case None => checks ::: List(RequestBuilder.DefaultHttpCheck)
      case _    => checks
    }

    val resolvedFollowRedirect = protocol.responsePart.followRedirect && httpAttributes.followRedirect

    val resolvedResponseTransformer = httpAttributes.responseTransformer.orElse(protocol.responsePart.responseTransformer)

    val resolvedResources = httpAttributes.explicitResources.map(_.build(protocol, throttled))

    val resolvedExtraInfoExtractor = httpAttributes.extraInfoExtractor.orElse(protocol.responsePart.extraInfoExtractor)

    val resolvedRequestExpression = request(protocol)

    val resolvedSignatureCalculatorExpression = commonAttributes.signatureCalculator.orElse(protocol.requestPart.signatureCalculator)

    val resolvedDiscardResponseChunks = httpAttributes.discardResponseChunks && protocol.responsePart.discardResponseChunks

    HttpRequestDef(
      commonAttributes.requestName,
      resolvedRequestExpression,
      resolvedSignatureCalculatorExpression,
      HttpRequestConfig(
        checks = resolvedChecks,
        responseTransformer = resolvedResponseTransformer,
        extraInfoExtractor = resolvedExtraInfoExtractor,
        maxRedirects = protocol.responsePart.maxRedirects,
        throttled = throttled,
        silent = httpAttributes.silent,
        followRedirect = resolvedFollowRedirect,
        discardResponseChunks = resolvedDiscardResponseChunks,
        protocol = protocol,
        explicitResources = resolvedResources))
  }
}

class HttpRequestBuilder(commonAttributes: CommonAttributes, httpAttributes: HttpAttributes) extends AbstractHttpRequestBuilder[HttpRequestBuilder](commonAttributes, httpAttributes) {

  private[http] def newInstance(commonAttributes: CommonAttributes) = new HttpRequestBuilder(commonAttributes, httpAttributes)
  private[http] def newInstance(httpAttributes: HttpAttributes) = new HttpRequestBuilder(commonAttributes, httpAttributes)
  def request(protocol: HttpProtocol): Expression[Request] = new HttpRequestExpressionBuilder(commonAttributes, httpAttributes, protocol).build
}
