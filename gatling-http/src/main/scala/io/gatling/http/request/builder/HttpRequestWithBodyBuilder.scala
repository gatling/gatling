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
import java.net.URI

import com.ning.http.client.RequestBuilder
import com.ning.http.multipart.Part

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation._
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request._

case class BodyAttributes(body: Option[Body] = None, bodyParts: List[BodyPart] = Nil)

object AbstractHttpRequestWithBodyBuilder {
	val emptyPartListSuccess = List.empty[Part].success
}

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

	def body(bd: Body): B = newInstance(httpAttributes, bodyAttributes.copy(body = Some(bd)))

	def processRequestBody(processor: Body => Body): B = newInstance(httpAttributes, bodyAttributes.copy(body = bodyAttributes.body.map(processor)))

	def bodyPart(bodyPart: BodyPart): B = newInstance(httpAttributes, bodyAttributes.copy(bodyParts = bodyPart :: bodyAttributes.bodyParts))

	protected def configureParts(session: Session, requestBuilder: RequestBuilder): Validation[RequestBuilder] = {
		require(!bodyAttributes.body.isDefined || bodyAttributes.bodyParts.isEmpty, "Can't have both a body and body parts!")

		if (bodyAttributes.body.isDefined)
			bodyAttributes.body.map(_.setBody(requestBuilder, session)).getOrElse(requestBuilder.success)

		else
			bodyAttributes.bodyParts.foldLeft(AbstractHttpRequestWithBodyBuilder.emptyPartListSuccess) { (parts, part) =>
				for {
					parts <- parts
					part <- part.toMultiPart(session)
				} yield part :: parts
			}.map { parts =>
				parts.foreach(requestBuilder.addBodyPart)
				requestBuilder
			}
	}

	protected override def getAHCRequestBuilder(session: Session, protocol: HttpProtocol): Validation[RequestBuilder] = {

		val requestBuilder = super.getAHCRequestBuilder(session, protocol)
		requestBuilder.flatMap(configureParts(session, _))
	}
}

object HttpRequestWithBodyBuilder {

	def apply(method: String, requestName: Expression[String], urlOrURI: Either[Expression[String], URI]) = new HttpRequestWithBodyBuilder(HttpAttributes(requestName, method, urlOrURI), BodyAttributes())
}

class HttpRequestWithBodyBuilder(
	httpAttributes: HttpAttributes,
	bodyAttributes: BodyAttributes)
	extends AbstractHttpRequestWithBodyBuilder[HttpRequestWithBodyBuilder](httpAttributes, bodyAttributes) {

	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		bodyAttributes: BodyAttributes) = {
		new HttpRequestWithBodyBuilder(httpAttributes, bodyAttributes)
	}
}
