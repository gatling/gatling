/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.io.InputStream

import com.ning.http.client.RequestBuilder

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.Validation
import io.gatling.http.config.HttpProtocolConfiguration
import io.gatling.http.request.{ ByteArrayBody, ELTemplateBodies, HttpRequestBody, InputStreamBody, RawFileBodies, SspTemplateBodies, StringBodies }

/**
 * This class serves as model to HTTP request with a body
 *
 * @param httpAttributes the base HTTP attributes
 * @param body the body that should be added to the request
 */
abstract class AbstractHttpRequestWithBodyBuilder[B <: AbstractHttpRequestWithBodyBuilder[B]](
	httpAttributes: HttpAttributes,
	body: Option[HttpRequestBody])
	extends AbstractHttpRequestBuilder[B](httpAttributes) {

	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		body: Option[HttpRequestBody]): B

	private[http] def newInstance(httpAttributes: HttpAttributes): B = newInstance(httpAttributes, body)

	def body(bd: HttpRequestBody): B = newInstance(httpAttributes, Some(bd))

	def body(bd: Expression[String]): B = body(StringBodies.asString(bd))
	def bodyAsBytes(bd: Expression[String]): B = body(StringBodies.asBytes(bd))
	def bodyAsStream(bd: Expression[String]): B = body(StringBodies.asStream(bd))

	def rawFileBody(filePath: Expression[String]): B = body(RawFileBodies.asFile(filePath))
	def rawFileBodyAsString(filePath: Expression[String]): B = body(RawFileBodies.asString(filePath))
	def rawFileBodyAsBytes(filePath: Expression[String]): B = body(RawFileBodies.asBytes(filePath))
	def rawFileBodyAsStream(filePath: Expression[String]): B = body(RawFileBodies.asStream(filePath))

	def elTemplateBody(filePath: Expression[String]): B = body(ELTemplateBodies.asString(filePath))
	def elTemplateBodyAsBytes(filePath: Expression[String]): B = body(ELTemplateBodies.asBytes(filePath))
	def elTemplateBodyAsStream(filePath: Expression[String]): B = body(ELTemplateBodies.asStream(filePath))

	def sspTemplateBody(filePath: Expression[String], additionalAttributes: Map[String, Any] = Map.empty): B = body(SspTemplateBodies.asString(filePath, additionalAttributes))
	def sspTemplateBodyAsBytes(filePath: Expression[String], additionalAttributes: Map[String, Any] = Map.empty): B = body(SspTemplateBodies.asBytes(filePath, additionalAttributes))
	def sspTemplateBodyAsStream(filePath: Expression[String], additionalAttributes: Map[String, Any] = Map.empty): B = body(SspTemplateBodies.asStream(filePath, additionalAttributes))

	def byteArrayBody(byteArray: Expression[Array[Byte]]): B = body(new ByteArrayBody(byteArray))

	def inputStreamBody(is: Expression[InputStream]): B = body(new InputStreamBody(is))

	def processRequestBody(processor: HttpRequestBody => HttpRequestBody) = newInstance(httpAttributes, body.map(processor))

	protected override def getAHCRequestBuilder(session: Session, protocolConfiguration: HttpProtocolConfiguration): Validation[RequestBuilder] = {

		val requestBuilder = super.getAHCRequestBuilder(session, protocolConfiguration)
		body.map(b => requestBuilder.flatMap(b.setBody(_, session))).getOrElse(requestBuilder)
	}
}
