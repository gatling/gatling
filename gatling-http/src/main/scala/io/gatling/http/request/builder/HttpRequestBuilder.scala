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

import io.gatling.core.session._
import io.gatling.http.{ HeaderValues, HeaderNames }
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
  explicitResources: List[HttpRequestBuilder] = Nil,
  body: Option[Body] = None,
  bodyParts: List[BodyPart] = Nil,
  formParams: List[HttpParam] = Nil,
  extraInfoExtractor: Option[ExtraInfoExtractor] = None)

object HttpRequestBuilder {

  implicit def toActionBuilder(requestBuilder: HttpRequestBuilder) = new HttpRequestActionBuilder(requestBuilder)

  val MultipartFormDataValueExpression = HeaderValues.MultipartFormData.expression
  val ApplicationFormUrlEncodedValueExpression = HeaderValues.ApplicationFormUrlEncoded.expression
}

/**
 * This class serves as model for all HttpRequestBuilders
 *
 * @param httpAttributes the base HTTP attributes
 */
class HttpRequestBuilder(commonAttributes: CommonAttributes, val httpAttributes: HttpAttributes)
    extends RequestBuilder[HttpRequestBuilder](commonAttributes) {

  private[http] def newInstance(commonAttributes: CommonAttributes): HttpRequestBuilder = new HttpRequestBuilder(commonAttributes, httpAttributes)

  private[http] def newInstance(httpAttributes: HttpAttributes): HttpRequestBuilder = new HttpRequestBuilder(commonAttributes, httpAttributes)

  /**
   * Stops defining the request and adds checks on the response
   *
   * @param checks the checks that will be performed on the response
   */
  def check(checks: HttpCheck*): HttpRequestBuilder = newInstance(httpAttributes.copy(checks = httpAttributes.checks ::: checks.toList))

  /**
   * Ignore the default checks configured on HttpProtocol
   */
  def ignoreDefaultChecks: HttpRequestBuilder = newInstance(httpAttributes.copy(ignoreDefaultChecks = true))

  def silent: HttpRequestBuilder = newInstance(httpAttributes.copy(silent = Some(true)))

  def notSilent: HttpRequestBuilder = newInstance(httpAttributes.copy(silent = Some(false)))

  def disableFollowRedirect: HttpRequestBuilder = newInstance(httpAttributes.copy(followRedirect = false))

  def extraInfoExtractor(f: ExtraInfoExtractor): HttpRequestBuilder = newInstance(httpAttributes.copy(extraInfoExtractor = Some(f)))

  /**
   * @param responseTransformer transforms the response before it's handled to the checks pipeline
   */
  def transformResponse(responseTransformer: PartialFunction[Response, Response]): HttpRequestBuilder = newInstance(httpAttributes.copy(responseTransformer = Some(responseTransformer)))

  def body(bd: Body): HttpRequestBuilder = newInstance(httpAttributes.copy(body = Some(bd)))

  def processRequestBody(processor: Body => Body): HttpRequestBuilder = newInstance(httpAttributes.copy(body = httpAttributes.body.map(processor)))

  def bodyPart(bodyPart: BodyPart): HttpRequestBuilder = newInstance(httpAttributes.copy(bodyParts = bodyPart :: httpAttributes.bodyParts))

  def resources(res: HttpRequestBuilder*): HttpRequestBuilder = newInstance(httpAttributes.copy(explicitResources = res.toList))

  def disableResponseChunksDiscarding = newInstance(httpAttributes.copy(discardResponseChunks = false))

  /**
   * Adds Content-Type header to the request set with "multipart/form-data" value
   */
  def asMultipartForm = header(HeaderNames.ContentType, HttpRequestBuilder.MultipartFormDataValueExpression)
  def asFormUrlEncoded = header(HeaderNames.ContentType, HttpRequestBuilder.ApplicationFormUrlEncodedValueExpression)

  def formParam(key: Expression[String], value: Expression[Any]): HttpRequestBuilder = formParam(SimpleParam(key, value))
  def multivaluedFormParam(key: Expression[String], values: Expression[Seq[Any]]) = formParam(MultivaluedParam(key, values))

  def formParamSeq(seq: Seq[(String, Any)]): HttpRequestBuilder = formParamSeq(seq2SeqExpression(seq))
  def formParamSeq(seq: Expression[Seq[(String, Any)]]): HttpRequestBuilder = formParam(ParamSeq(seq))

  def formParamMap(map: Map[String, Any]): HttpRequestBuilder = formParamSeq(map2SeqExpression(map))
  def formParamMap(map: Expression[Map[String, Any]]): HttpRequestBuilder = formParam(ParamMap(map))

  private def formParam(formParam: HttpParam): HttpRequestBuilder =
    newInstance(httpAttributes.copy(formParams = httpAttributes.formParams ::: List(formParam))).asFormUrlEncoded

  def formUpload(name: Expression[String], filePath: Expression[String]) = {

    val file = RawFileBodies.asFile(filePath)
    val fileName = file.map(_.getName)

    bodyPart(BodyPart.fileBodyPart(Some(name), file).fileName(fileName)).asMultipartForm
  }

  def request(protocol: HttpProtocol): Expression[Request] = new HttpRequestExpressionBuilder(commonAttributes, httpAttributes, protocol).build

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

    val resolvedChecks =
      if (checks.exists(_.scope == Status))
        checks ::: List(RequestBuilder.DefaultHttpCheck)
      else
        checks

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
