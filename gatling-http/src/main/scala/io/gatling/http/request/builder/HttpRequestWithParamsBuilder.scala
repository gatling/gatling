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

import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.core.session.el.EL
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.{ BodyPart, RawFileBodies }

object HttpRequestWithParamsBuilder {
  val multipartFormDataValueExpression = HeaderValues.MULTIPART_FORM_DATA.el[String]
}

/**
 * This class serves as model to HTTP request with a body and parameters
 *
 * @param commonAttributes the CommonAttributes
 * @param httpAttributes the HttpAttributes
 * @param params the parameters that should be added to the request
 */
class HttpRequestWithParamsBuilder(
  commonAttributes: CommonAttributes,
  httpAttributes: HttpAttributes,
  params: List[HttpParam])
    extends AbstractHttpRequestBuilder[HttpRequestWithParamsBuilder](commonAttributes, httpAttributes) {

  private[http] def newInstance(commonAttributes: CommonAttributes) = new HttpRequestWithParamsBuilder(commonAttributes, httpAttributes, params)
  private[http] def newInstance(httpAttributes: HttpAttributes) = new HttpRequestWithParamsBuilder(commonAttributes, httpAttributes, params)

  /**
   * Adds Content-Type header to the request set with "multipart/form-data" value
   */
  def asMultipartForm: HttpRequestWithParamsBuilder = header(HeaderNames.CONTENT_TYPE, HttpRequestWithParamsBuilder.multipartFormDataValueExpression)

  def param(key: Expression[String], value: Expression[Any]): HttpRequestWithParamsBuilder = param(SimpleParam(key, value))
  def multivaluedParam(key: Expression[String], values: Expression[Seq[Any]]): HttpRequestWithParamsBuilder = param(MultivaluedParam(key, values))
  def paramsSeq(seq: Expression[Seq[(String, Any)]]): HttpRequestWithParamsBuilder = param(ParamSeq(seq))
  def paramsMap(map: Expression[Map[String, Any]]): HttpRequestWithParamsBuilder = param(ParamMap(map))
  private def param(param: HttpParam): HttpRequestWithParamsBuilder = new HttpRequestWithParamsBuilder(commonAttributes, httpAttributes, param :: params)

  def formUpload(name: Expression[String], filePath: Expression[String]) = {

    val file = RawFileBodies.asFile(filePath)
    val fileName = file.map(_.getName)

    bodyPart(BodyPart.fileBodyPart(name, file).fileName(fileName)).asMultipartForm
  }

  def ahcRequest(protocol: HttpProtocol) = new HttpRequestWithParamsExpressionBuilder(commonAttributes, httpAttributes, params, protocol).build
}
