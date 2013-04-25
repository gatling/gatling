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

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.{ EL, Expression, Session }
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import io.gatling.http.Headers.{ Names => HeaderNames, Values => HeaderValues }
import io.gatling.http.config.HttpProtocolConfiguration
import io.gatling.http.request.RequestBody
import io.gatling.http.util.HttpHelper
import com.ning.http.client.{ RequestBuilder, StringPart }
import com.ning.http.client.FluentStringsMap

case class HttpParamsAttributes(
	params: List[HttpParam] = Nil,
	uploadedFiles: List[UploadedFile] = Nil)

object AbstractHttpRequestWithBodyAndParamsBuilder {
	val multipartHeaderValueExpression = EL.compile[String](HeaderValues.MULTIPART_FORM_DATA)
}

/**
 * This class serves as model to HTTP request with a body and parameters
 *
 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
 * @param body the body that should be added to the request
 * @param params the parameters that should be added to the request
 */
abstract class AbstractHttpRequestWithBodyAndParamsBuilder[B <: AbstractHttpRequestWithBodyAndParamsBuilder[B]](
	httpAttributes: HttpAttributes,
	body: Option[RequestBody],
	paramsAttributes: HttpParamsAttributes)
	extends AbstractHttpRequestWithBodyBuilder[B](httpAttributes, body) {

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param params the parameters that should be added to the request
	 * @param body the body that should be added to the request
	 * @param paramsAttributes the attributes for requests with HTTP params
	 */
	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		body: Option[RequestBody],
		paramsAttributes: HttpParamsAttributes): B

	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		body: Option[RequestBody]): B = {
		newInstance(httpAttributes, body, paramsAttributes)
	}

	def param(key: Expression[String], value: Expression[String]): B = {
		val httpParam: HttpParam = (key, (s: Session) => value(s).map(Seq(_)))
		param(httpParam)
	}

	def multiValuedParam(key: Expression[String], values: Expression[Seq[String]]): B = {
		val httpParam: HttpParam = (key, values)
		param(httpParam)
	}

	private def param(param: HttpParam): B = newInstance(httpAttributes, body, paramsAttributes.copy(params = param :: paramsAttributes.params))

	def upload(paramKey: Expression[String], fileName: Expression[String], mimeType: String = HeaderValues.APPLICATION_OCTET_STREAM, charset: String = configuration.core.encoding): B =
		newInstance(httpAttributes, body, paramsAttributes.copy(uploadedFiles = new UploadedFile(paramKey, fileName, mimeType, charset) :: paramsAttributes.uploadedFiles))
			.header(HeaderNames.CONTENT_TYPE, AbstractHttpRequestWithBodyAndParamsBuilder.multipartHeaderValueExpression)

	protected override def getAHCRequestBuilder(session: Session, protocolConfiguration: HttpProtocolConfiguration): Validation[RequestBuilder] = {

		def configureParams(requestBuilder: RequestBuilder): Validation[RequestBuilder] =
			if (!paramsAttributes.params.isEmpty)
				// As a side effect, requestBuilder.setParameters() resets the body data, so, it should not be called with empty parameters 
				HttpHelper.httpParamsToFluentMap(paramsAttributes.params, session).map(requestBuilder.setParameters)
			else
				requestBuilder.success

		def configureFileParts(requestBuilder: RequestBuilder): Validation[RequestBuilder] = {

			val resolvedFileParts = paramsAttributes.uploadedFiles
				.map(_.filePart(session))
				.sequence

			resolvedFileParts.map { uploadedFiles =>
				uploadedFiles.foreach(requestBuilder.addBodyPart)
				requestBuilder
			}
		}

		def configureStringParts(requestBuilder: RequestBuilder): Validation[RequestBuilder] =
			HttpHelper.resolveParams(paramsAttributes.params, session).map { params =>
				for {
					(key, values) <- params
					value <- values
				} requestBuilder.addBodyPart(new StringPart(key, value))
				requestBuilder
			}

		val requestBuilder = super.getAHCRequestBuilder(session, protocolConfiguration)

		if (paramsAttributes.uploadedFiles.isEmpty)
			requestBuilder.flatMap(configureParams)
		else {
			requestBuilder.flatMap(configureStringParts).flatMap(configureFileParts)
		}
	}
}