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

package io.gatling.http.request.builder

import io.gatling.core.body.{ Body, RawFileBodies }
import io.gatling.core.session._
import io.gatling.http.request.BodyPart
import io.gatling.internal.quicklens._

import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaderValues }

object BodyAttributes {
  val Empty: BodyAttributes = BodyAttributes(body = None, bodyParts = Nil, formParams = Nil, form = None, headersBuiltIn = None)

  sealed abstract class HeadersBuiltIn(contentHeaderValue: Expression[String]) {

    private def containsCaseInsensitive(map: Map[CharSequence, _], key: CharSequence): Boolean =
      map.keys.exists(_.toString.equalsIgnoreCase(key.toString))
    def patch(rawHeaders: Map[CharSequence, Expression[String]], hasBody: Boolean): Map[CharSequence, Expression[String]] =
      rawHeaders ++
        (if (!containsCaseInsensitive(rawHeaders, HttpHeaderNames.ACCEPT)) {
           Map(HttpHeaderNames.ACCEPT -> contentHeaderValue)
         } else {
           Map.empty
         }) ++
        (if (hasBody && !containsCaseInsensitive(rawHeaders, HttpHeaderNames.CONTENT_TYPE)) {
           Map(HttpHeaderNames.CONTENT_TYPE -> contentHeaderValue)
         } else {
           Map.empty
         })
  }
  object HeadersBuiltIn {
    case object AsJson extends HeadersBuiltIn(HttpHeaderValues.APPLICATION_JSON.toString.expressionSuccess)
    case object AsXml extends HeadersBuiltIn(HttpHeaderValues.APPLICATION_XML.toString.expressionSuccess)
  }
}

final case class BodyAttributes(
    body: Option[Body],
    bodyParts: List[BodyPart],
    formParams: List[HttpParam],
    form: Option[Expression[Map[String, Any]]],
    headersBuiltIn: Option[BodyAttributes.HeadersBuiltIn]
)

object RequestWithBodyBuilder {
  private val MultipartFormDataValueExpression = HttpHeaderValues.MULTIPART_FORM_DATA.toString.expressionSuccess
  private val ApplicationFormUrlEncodedValueExpression = HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString.expressionSuccess
}

abstract class RequestWithBodyBuilder[B <: RequestWithBodyBuilder[B]] extends RequestBuilder[B] {

  protected def bodyAttributes: BodyAttributes

  protected def newInstance(bodyAttributes: BodyAttributes): B

  def body(bd: Body): B = newInstance(bodyAttributes.copy(body = Some(bd)))

  def processRequestBody(processor: Body => Body): B = newInstance(bodyAttributes.modify(_.body)(_.map(processor)))

  def bodyPart(part: BodyPart): B = newInstance(bodyAttributes.modify(_.bodyParts)(_ ::: List(part)))

  def bodyParts(parts: BodyPart*): B = {
    require(parts.nonEmpty, "bodyParts can't be empty.")
    require(!parts.contains(null), "bodyParts can't contain null elements. Forward reference issue?")
    newInstance(bodyAttributes.modify(_.bodyParts)(_ ::: parts.toList))
  }

  /**
   * Adds Content-Type header to the request set with "multipart/form-data" value
   */
  def asMultipartForm: B = header(HttpHeaderNames.CONTENT_TYPE, RequestWithBodyBuilder.MultipartFormDataValueExpression)

  def asFormUrlEncoded: B = header(HttpHeaderNames.CONTENT_TYPE, RequestWithBodyBuilder.ApplicationFormUrlEncodedValueExpression)

  def formParam(key: Expression[String], value: Expression[Any]): B = formParam(SimpleParam(key, value))

  def multivaluedFormParam(key: Expression[String], values: Expression[Seq[Any]]): B = formParam(MultivaluedParam(key, values))

  def formParamSeq(seq: Seq[(String, Any)]): B = formParamSeq(tupleSeq2SeqExpression(seq))

  def formParamSeq(seq: Expression[Seq[(String, Any)]]): B = formParam(ParamSeq(seq))

  def formParamMap(map: Map[String, Any]): B = formParamSeq(tupleSeq2SeqExpression(map.toSeq))

  def formParamMap(map: Expression[Map[String, Any]]): B = formParam(ParamMap(map))

  private def formParam(formParam: HttpParam): B =
    newInstance(bodyAttributes.modify(_.formParams)(_ ::: List(formParam)))

  def form(form: Expression[Map[String, Any]]): B =
    newInstance(bodyAttributes.modify(_.form).setTo(Some(form)))

  def formUpload(name: Expression[String], filePath: Expression[String])(implicit rawFileBodies: RawFileBodies): B =
    bodyPart(BodyPart.rawFileBodyPart(Some(name), filePath, rawFileBodies))

  /**
   * Adds Accept and Content-Type headers to the request set with "application/json" values
   */
  def asJson: B = newInstance(modify(bodyAttributes)(_.headersBuiltIn).setTo(Some(BodyAttributes.HeadersBuiltIn.AsJson)))

  /**
   * Adds Accept and Content-Type headers to the request set with "application/xml" values
   */
  def asXml: B = newInstance(modify(bodyAttributes)(_.headersBuiltIn).setTo(Some(BodyAttributes.HeadersBuiltIn.AsXml)))
}
