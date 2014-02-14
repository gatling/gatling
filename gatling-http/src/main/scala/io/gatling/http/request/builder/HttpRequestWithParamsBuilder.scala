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

import io.gatling.core.session.{ Expression, RichExpression, Session }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.{ FileBodyPart, RawFileBodies }
import io.gatling.http.request.builder.ahc.AHCHttpRequestWithParamsBuilder

/**
 * This class serves as model to HTTP request with a body and parameters
 *
 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
 * @param body the body that should be added to the request
 * @param params the parameters that should be added to the request
 */
class HttpRequestWithParamsBuilder(
	commonAttributes: CommonAttributes,
	httpAttributes: HttpAttributes,
	params: List[HttpParam])
	extends AbstractHttpRequestBuilder[HttpRequestWithParamsBuilder](commonAttributes, httpAttributes) {

	private[http] def newInstance(commonAttributes: CommonAttributes) = new HttpRequestWithParamsBuilder(commonAttributes, httpAttributes, params)
	private[http] def newInstance(httpAttributes: HttpAttributes) = new HttpRequestWithParamsBuilder(commonAttributes, httpAttributes, params)

	def param(key: Expression[String], value: Expression[Any]): HttpRequestWithParamsBuilder = param(SimpleParam(key, value))
	def multivaluedParam(key: Expression[String], values: Expression[Seq[Any]]): HttpRequestWithParamsBuilder = param(MultivaluedParam(key, values))
	def paramsSequence(seq: Expression[Seq[(String, Any)]]): HttpRequestWithParamsBuilder = param(ParamSeq(seq))
	def paramsMap(map: Expression[Map[String, Any]]): HttpRequestWithParamsBuilder = param(ParamMap(map))
	private def param(param: HttpParam): HttpRequestWithParamsBuilder = new HttpRequestWithParamsBuilder(commonAttributes, httpAttributes, param :: params)

	def formUpload(name: Expression[String], filePath: Expression[String]) = {

		val file = RawFileBodies.asFile(filePath)
		val filename = file.map(_.getName)

		bodyPart(FileBodyPart(name, file, _fileName = Some(filename))).asMultipartForm
	}

	override def newAHCRequestBuilder(session: Session, protocol: HttpProtocol) = new AHCHttpRequestWithParamsBuilder(commonAttributes, httpAttributes, params, session, protocol)
}
