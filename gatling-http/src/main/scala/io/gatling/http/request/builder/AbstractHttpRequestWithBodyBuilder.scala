/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.io.{ File, InputStream }

import com.ning.http.client.RequestBuilder

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ Validation, ValidationList }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request._

case class BodyAttributes(body: Option[Body] = None, bodyParts: List[BodyPart] = Nil)

/**
 * This class serves as model to HTTP request with a body
 *
 * @param httpAttributes the base HTTP attributes
 * @param body the body that should be added to the request
 */
abstract class AbstractHttpRequestWithBodyBuilder[B <: AbstractHttpRequestWithBodyBuilder[B]](
	httpAttributes: HttpAttributes,
	bodyAttributes: BodyAttributes)
	extends AbstractHttpRequestBuilder[B](httpAttributes) {

	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		bodyAttributes: BodyAttributes): B

	private[http] def newInstance(httpAttributes: HttpAttributes): B = newInstance(httpAttributes, bodyAttributes)

	def requestBody(bd: Body): B = newInstance(httpAttributes, bodyAttributes.copy(body = Some(bd)))

	// String
	def body(bd: Expression[String]): B = requestBody(StringBody(bd))
	def rawFileBodyAsString(filePath: Expression[String]): B = body(RawFileBodies.asString(filePath))
	def elFileBody(filePath: Expression[String]): B = body(ELFileBodies.asString(filePath))
	@deprecated("Scalate support will be dropped in 2.1.0, prefer EL files or plain scala code", "2.0.0")
	def sspFileBody(filePath: Expression[String], additionalAttributes: Map[String, Any] = Map.empty): B = body(SspFileBodies.asString(filePath, additionalAttributes))

	// Bytes
	def byteArrayBody(byteArray: Expression[Array[Byte]]): B = requestBody(new ByteArrayBody(byteArray))
	def rawFileBodyAsBytes(filePath: Expression[String]): B = byteArrayBody(RawFileBodies.asBytes(filePath))
	def elFileBodyAsBytes(filePath: Expression[String]): B = byteArrayBody(ELFileBodies.asBytes(filePath))
	def bodyAsBytes(bd: Expression[String]): B = byteArrayBody(StringBodies.asBytes(bd))
	@deprecated("Scalate support will be dropped in 2.1.0, prefer EL files or plain scala code", "2.0.0")
	def sspFileBodyAsBytes(filePath: Expression[String], additionalAttributes: Map[String, Any] = Map.empty): B = byteArrayBody(SspFileBodies.asBytes(filePath, additionalAttributes))

	// File
	def rawFileBody(filePath: Expression[String]): B = requestBody(RawFileBody(RawFileBodies.asFile(filePath)))

	// InputStream
	def inputStreamBody(is: Expression[InputStream]): B = requestBody(new InputStreamBody(is))

	def processBody(processor: Body => Body): B = newInstance(httpAttributes, bodyAttributes.copy(body = bodyAttributes.body.map(processor)))

	def bodyPart(bodyPart: BodyPart): B = newInstance(httpAttributes, bodyAttributes.copy(bodyParts = bodyPart :: bodyAttributes.bodyParts))
	def bodyPart(name: Expression[String], value: Expression[String]): B = bodyPart(StringBodyPart(name, value))
	def rawFileBodyPart(name: Expression[String], filePath: Expression[String], mimeType: String) = bodyPart(FileBodyPart(name, RawFileBodies.asFile(filePath), mimeType))
	def byteArrayBodyPart(name: Expression[String], data: Expression[Array[Byte]], mimeType: String) = bodyPart(ByteArrayBodyPart(name, data, mimeType))
	def elFileBodyPart(name: Expression[String], filePath: Expression[String]): B = bodyPart(name, ELFileBodies.asString(filePath))
	def elFileBodyPart(name: Expression[String], filePath: Expression[String], mimeType: String): B = byteArrayBodyPart(name, ELFileBodies.asBytes(filePath), mimeType)
	@deprecated("Scalate support will be dropped in 2.1.0, prefer EL files or plain scala code", "2.0.0")
	def sspFileBodyPart(name: Expression[String], filePath: Expression[String], mimeType: String, additionalAttributes: Map[String, Any] = Map.empty): B = byteArrayBodyPart(name, SspFileBodies.asBytes(filePath, additionalAttributes), mimeType)
	
	protected override def getAHCRequestBuilder(session: Session, protocol: HttpProtocol): Validation[RequestBuilder] = {

		val requestBuilder = super.getAHCRequestBuilder(session, protocol)

		require(!bodyAttributes.body.isDefined || bodyAttributes.bodyParts.isEmpty, "Can't have both a body and body parts!")

		if (bodyAttributes.body.isDefined)
			bodyAttributes.body.map(b => requestBuilder.flatMap(b.setBody(_, session))).getOrElse(requestBuilder)

		else {
			for {
				parts <- bodyAttributes.bodyParts.reverse.map(_.toPart(session)).sequence
				requestBuilder <- requestBuilder
			} yield {
				parts.foreach(requestBuilder.addBodyPart)
				requestBuilder
			}
		}
	}
}
