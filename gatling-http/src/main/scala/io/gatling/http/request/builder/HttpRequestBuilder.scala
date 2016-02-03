/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.http.request.builder

import io.gatling.core.CoreComponents
import io.gatling.core.body.{ Body, RawFileBodies }
import io.gatling.core.session._
import io.gatling.http.action.sync.HttpRequestActionBuilder
import io.gatling.http.{ HeaderValues, HeaderNames }
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckScope.Status
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request._
import io.gatling.http.response.Response

import com.softwaremill.quicklens._

case class HttpAttributes(
  checks:                List[HttpCheck]                              = Nil,
  ignoreDefaultChecks:   Boolean                                      = false,
  silent:                Option[Boolean]                              = None,
  followRedirect:        Boolean                                      = true,
  discardResponseChunks: Boolean                                      = true,
  responseTransformer:   Option[PartialFunction[Response, Response]]  = None,
  explicitResources:     List[HttpRequestBuilder]                     = Nil,
  body:                  Option[Body]                                 = None,
  bodyParts:             List[BodyPart]                               = Nil,
  formParams:            List[HttpParam]                              = Nil,
  form:                  Option[Expression[Map[String, Seq[String]]]] = None,
  extraInfoExtractor:    Option[ExtraInfoExtractor]                   = None
)

object HttpRequestBuilder {

  implicit def toActionBuilder(requestBuilder: HttpRequestBuilder): HttpRequestActionBuilder =
    new HttpRequestActionBuilder(requestBuilder)

  val MultipartFormDataValueExpression = HeaderValues.MultipartFormData.expressionSuccess
  val ApplicationFormUrlEncodedValueExpression = HeaderValues.ApplicationFormUrlEncoded.expressionSuccess
}

/**
 * This class serves as model for all HttpRequestBuilders
 *
 * @param httpAttributes the base HTTP attributes
 */
case class HttpRequestBuilder(commonAttributes: CommonAttributes, httpAttributes: HttpAttributes) extends RequestBuilder[HttpRequestBuilder] {

  private[http] def newInstance(commonAttributes: CommonAttributes): HttpRequestBuilder = new HttpRequestBuilder(commonAttributes, httpAttributes)

  /**
   * Stops defining the request and adds checks on the response
   *
   * @param checks the checks that will be performed on the response
   */
  def check(checks: HttpCheck*): HttpRequestBuilder = this.modify(_.httpAttributes.checks).using(_ ::: checks.toList)

  /**
   * Ignore the default checks configured on HttpProtocol
   */
  def ignoreDefaultChecks: HttpRequestBuilder = this.modify(_.httpAttributes.ignoreDefaultChecks).setTo(true)

  def silent: HttpRequestBuilder = this.modify(_.httpAttributes.silent).setTo(Some(true))

  def notSilent: HttpRequestBuilder = this.modify(_.httpAttributes.silent).setTo(Some(false))

  def disableFollowRedirect: HttpRequestBuilder = this.modify(_.httpAttributes.followRedirect).setTo(false)

  def extraInfoExtractor(f: ExtraInfoExtractor): HttpRequestBuilder = this.modify(_.httpAttributes.extraInfoExtractor).setTo(Some(f))

  /**
   * @param responseTransformer transforms the response before it's handled to the checks pipeline
   */
  def transformResponse(responseTransformer: PartialFunction[Response, Response]): HttpRequestBuilder = this.modify(_.httpAttributes.responseTransformer).setTo(Some(responseTransformer))

  def body(bd: Body): HttpRequestBuilder = this.modify(_.httpAttributes.body).setTo(Some(bd))

  def processRequestBody(processor: Body => Body): HttpRequestBuilder = this.modify(_.httpAttributes.body).using(_.map(processor))

  def bodyPart(bodyPart: BodyPart): HttpRequestBuilder = this.modify(_.httpAttributes.bodyParts).using(_ ::: List(bodyPart))

  def resources(res: HttpRequestBuilder*): HttpRequestBuilder = this.modify(_.httpAttributes.explicitResources).setTo(res.toList)

  def disableResponseChunksDiscarding = this.modify(_.httpAttributes.discardResponseChunks).setTo(false)

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
    this.modify(_.httpAttributes.formParams).using(_ ::: List(formParam))

  def form(form: Expression[Map[String, Seq[String]]]): HttpRequestBuilder =
    this.modify(_.httpAttributes.form).setTo(Some(form))

  def formUpload(name: Expression[String], filePath: Expression[String])(implicit rawFileBodies: RawFileBodies) =
    bodyPart(BodyPart.rawFileBodyPart(Some(name), filePath))

  /**
   * This method builds the request that will be sent
   *
   * @param coreComponents the CoreComponents
   * @param httpComponents the HttpComponents
   * @param throttled if throttling is enabled
   */
  def build(coreComponents: CoreComponents, httpComponents: HttpComponents, throttled: Boolean): HttpRequestDef = {

    val httpProtocol = httpComponents.httpProtocol

    val checks =
      if (httpAttributes.ignoreDefaultChecks)
        httpAttributes.checks
      else
        httpProtocol.responsePart.checks ::: httpAttributes.checks

    val resolvedChecks =
      if (checks.exists(_.scope == Status))
        checks
      else
        checks ::: List(RequestBuilder.DefaultHttpCheck)

    val resolvedFollowRedirect = httpProtocol.responsePart.followRedirect && httpAttributes.followRedirect

    val resolvedResponseTransformer = httpAttributes.responseTransformer.orElse(httpProtocol.responsePart.responseTransformer)

    val resolvedResources = httpAttributes.explicitResources.map(_.build(coreComponents, httpComponents, throttled))

    val resolvedExtraInfoExtractor = httpAttributes.extraInfoExtractor.orElse(httpProtocol.responsePart.extraInfoExtractor)

    val resolvedRequestExpression = new HttpRequestExpressionBuilder(commonAttributes, httpAttributes, coreComponents, httpComponents).build

    val resolvedDiscardResponseChunks = httpAttributes.discardResponseChunks && httpProtocol.responsePart.discardResponseChunks

    HttpRequestDef(
      commonAttributes.requestName,
      resolvedRequestExpression,
      HttpRequestConfig(
        checks = resolvedChecks,
        responseTransformer = resolvedResponseTransformer,
        extraInfoExtractor = resolvedExtraInfoExtractor,
        maxRedirects = httpProtocol.responsePart.maxRedirects,
        throttled = throttled,
        silent = httpAttributes.silent,
        followRedirect = resolvedFollowRedirect,
        discardResponseChunks = resolvedDiscardResponseChunks,
        coreComponents = coreComponents,
        httpComponents = httpComponents,
        explicitResources = resolvedResources
      )
    )
  }
}
