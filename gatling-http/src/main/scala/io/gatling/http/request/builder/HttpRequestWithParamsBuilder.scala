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

import io.gatling.core.session.{ Expression, ExpressionWrapper, RichExpression }
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.{ BodyPart, RawFileBodies }
import com.ning.http.client.Request

object HttpRequestWithParamsBuilder {
  val MultipartFormDataValueExpression = HeaderValues.MultipartFormData.expression
  val ApplicationFormUrlEncodedValueExpression = HeaderValues.ApplicationFormUrlEncoded.expression
}

/**
 * This class serves as model to HTTP request with a body and parameters
 *
 * @param commonAttributes the CommonAttributes
 * @param httpAttributes the HttpAttributes
 * @param formParams the form parameters that should be added to the request
 */
class HttpRequestWithParamsBuilder(
  commonAttributes: CommonAttributes,
  httpAttributes: HttpAttributes,
  formParams: List[HttpParam])
    extends AbstractHttpRequestBuilder[HttpRequestWithParamsBuilder](commonAttributes, httpAttributes) {

  private[http] def newInstance(commonAttributes: CommonAttributes) = new HttpRequestWithParamsBuilder(commonAttributes, httpAttributes, formParams)
  private[http] def newInstance(httpAttributes: HttpAttributes) = new HttpRequestWithParamsBuilder(commonAttributes, httpAttributes, formParams)

  /**
   * Adds Content-Type header to the request set with "multipart/form-data" value
   */
  def asMultipartForm: HttpRequestWithParamsBuilder = header(HeaderNames.ContentType, HttpRequestWithParamsBuilder.MultipartFormDataValueExpression)

  def formParam(key: Expression[String], value: Expression[Any]): HttpRequestWithParamsBuilder = formParam(SimpleParam(key, value))
  def multivaluedFormParam(key: Expression[String], values: Expression[Seq[Any]]): HttpRequestWithParamsBuilder = formParam(MultivaluedParam(key, values))

  def formParamSeq(seq: Seq[(String, Any)]): HttpRequestWithParamsBuilder = formParamSeq(seq2SeqExpression(seq))
  def formParamSeq(seq: Expression[Seq[(String, Any)]]): HttpRequestWithParamsBuilder = formParam(ParamSeq(seq))

  def formParamMap(map: Map[String, Any]): HttpRequestWithParamsBuilder = formParamSeq(map2SeqExpression(map))
  def formParamMap(map: Expression[Map[String, Any]]): HttpRequestWithParamsBuilder = formParam(ParamMap(map))

  private def formParam(formParam: HttpParam): HttpRequestWithParamsBuilder =
    new HttpRequestWithParamsBuilder(commonAttributes, httpAttributes, formParam :: formParams).asMultipartForm

  def formUpload(name: Expression[String], filePath: Expression[String]) = {

    val file = RawFileBodies.asFile(filePath)
    val fileName = file.map(_.getName)

    bodyPart(BodyPart.fileBodyPart(name, file).fileName(fileName)).asMultipartForm
  }

  def request(protocol: HttpProtocol): Expression[Request] = new HttpRequestWithParamsExpressionBuilder(commonAttributes, httpAttributes, formParams, protocol).build
}
