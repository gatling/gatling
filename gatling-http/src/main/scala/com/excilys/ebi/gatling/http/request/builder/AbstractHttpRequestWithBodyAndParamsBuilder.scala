/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.http.request.builder

import scala.collection.JavaConversions._

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.session.{ Expression, Session }
import com.excilys.ebi.gatling.http.Headers.{ Names => HeaderNames, Values => HeaderValues }
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.util.HttpHelper
import com.ning.http.client.{ Realm, RequestBuilder, StringPart }
import com.ning.http.client.FilePart
import com.ning.http.client.FluentStringsMap

import scalaz.Scalaz.{ ToTraverseOps, ToValidationV, listInstance, stringInstance }
import scalaz.Validation

case class HttpParamsAttributes(
	params: List[HttpParam] = Nil,
	uploadedFiles: List[UploadedFile] = Nil)

/**
 * This class serves as model to HTTP request with a body and parameters
 *
 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
 * @param body the body that should be added to the request
 * @param params the parameters that should be added to the request
 */
abstract class AbstractHttpRequestWithBodyAndParamsBuilder[B <: AbstractHttpRequestWithBodyAndParamsBuilder[B]](
	httpAttributes: HttpAttributes,
	body: Option[HttpRequestBody],
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
		body: Option[HttpRequestBody],
		paramsAttributes: HttpParamsAttributes): B

	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		body: Option[HttpRequestBody]): B = {
		newInstance(httpAttributes, body, paramsAttributes)
	}

	def param(key: String): B = param(Expression[String](key), (s: Session) => s.safeGetAs[String](key))

	def param(key: Expression[String], value: Expression[String]): B = {
		val httpParam: HttpParam = (key, (s: Session) => value(s).map(Seq(_)))
		param(httpParam)
	}

	def multiValuedParam(key: String): B = multiValuedParam(Expression[String](key), (s: Session) => s.safeGetAs[Seq[String]](key))

	def multiValuedParam(key: Expression[String], values: Seq[String]): B = {
		val httpParam: HttpParam = (key, (s: Session) => values.success)
		param(httpParam)
	}

	def multiValuedParam(key: Expression[String], values: Expression[Seq[String]]): B = {
		val httpParam: HttpParam = (key, values)
		param(httpParam)
	}

	private def param(param: HttpParam): B = newInstance(httpAttributes, body, paramsAttributes.copy(params = param :: paramsAttributes.params))

	def upload(paramKey: Expression[String], fileName: Expression[String], mimeType: String = HeaderValues.APPLICATION_OCTET_STREAM, charset: String = configuration.simulation.encoding): B =
		newInstance(httpAttributes, body, paramsAttributes.copy(uploadedFiles = new UploadedFile(paramKey, fileName, mimeType, charset) :: paramsAttributes.uploadedFiles))
			.header(HeaderNames.CONTENT_TYPE, HeaderValues.MULTIPART_FORM_DATA)

	protected override def getAHCRequestBuilder(session: Session, protocolConfiguration: HttpProtocolConfiguration): Validation[String, RequestBuilder] = {

		def configureParams(requestBuilder: RequestBuilder): Validation[String, RequestBuilder] =
			HttpHelper.httpParamsToFluentMap(paramsAttributes.params, session).map(requestBuilder.setParameters)

		def configureFileParts(requestBuilder: RequestBuilder): Validation[String, RequestBuilder] = {

			val resolvedFileParts = paramsAttributes.uploadedFiles
				.map(_.filePart(session))
				.toList
				.sequence[({ type l[a] = Validation[String, a] })#l, FilePart]

			resolvedFileParts.map { uploadedFiles =>
				uploadedFiles.foreach(requestBuilder.addBodyPart)
				requestBuilder
			}
		}

		def configureStringParts(requestBuilder: RequestBuilder): Validation[String, RequestBuilder] = {
			HttpHelper.httpParamsToFluentMap(paramsAttributes.params, session).map { map: FluentStringsMap =>
				map.iterator.foreach { entry => entry.getValue.foreach(value => requestBuilder.addBodyPart(new StringPart(entry.getKey, value))) }
				requestBuilder
			}
		}

		val requestBuilder = super.getAHCRequestBuilder(session, protocolConfiguration)

		if (paramsAttributes.uploadedFiles.isEmpty)
			requestBuilder.flatMap(configureParams)
		else {
			requestBuilder.flatMap(configureStringParts).flatMap(configureFileParts)
		}
	}

}