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

import scala.collection.JavaConversions.asJavaCollection
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.session.{ EvaluatableString, EvaluatableStringSeq, Session }
import com.excilys.ebi.gatling.core.session.ELParser.parseEL
import com.excilys.ebi.gatling.core.session.Session.{ attributeAsEvaluatableString, attributeAsEvaluatableStringSeq, evaluatableStringToEvaluatableStringSeq }
import com.excilys.ebi.gatling.http.Headers.{ Names => HeaderNames, Values => HeaderValues }
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.util.HttpHelper.httpParamsToFluentMap
import com.ning.http.client.{ Realm, RequestBuilder, StringPart }

case class HttpParamsAttributes(
	params: List[HttpParam],
	uploadedFiles: List[UploadedFile])

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

	protected override def getAHCRequestBuilder(session: Session, protocolConfiguration: HttpProtocolConfiguration): RequestBuilder = {
		val requestBuilder = super.getAHCRequestBuilder(session, protocolConfiguration)
		if (paramsAttributes.uploadedFiles.isEmpty)
			configureParams(requestBuilder, session)
		else {
			configureStringParts(requestBuilder, session)
			configureBodyParts(requestBuilder, paramsAttributes.uploadedFiles, session)
		}

		requestBuilder
	}

	def param(key: String): B = param(parseEL(key), attributeAsEvaluatableString(key))

	def param(key: EvaluatableString, value: EvaluatableString): B = {
		val httpParam: HttpParam = (key, evaluatableStringToEvaluatableStringSeq(value))
		param(httpParam)
	}

	def multiValuedParam(key: String): B = multiValuedParam(parseEL(key), key)

	def multiValuedParam(key: EvaluatableString, value: String): B = {
		val httpParam: HttpParam = (key, attributeAsEvaluatableStringSeq(value))
		param(httpParam)
	}

	def multiValuedParam(key: EvaluatableString, values: Seq[String]): B = {
		val httpParam: HttpParam = (key, (s: Session) => values)
		param(httpParam)
	}

	def multiValuedParam(key: EvaluatableString, values: EvaluatableStringSeq): B = {
		val httpParam: HttpParam = (key, values)
		param(httpParam)
	}

	private def param(param: HttpParam): B = newInstance(httpAttributes, body, paramsAttributes.copy(params = param :: paramsAttributes.params))

	def upload(paramKey: EvaluatableString, fileName: EvaluatableString, mimeType: String = HeaderValues.APPLICATION_OCTET_STREAM, charset: String = configuration.simulation.encoding): B =

		newInstance(httpAttributes, body, paramsAttributes.copy(uploadedFiles = new UploadedFile(paramKey, fileName, mimeType, charset) :: paramsAttributes.uploadedFiles))
			.header(HeaderNames.CONTENT_TYPE, HeaderValues.MULTIPART_FORM_DATA)

	/**
	 * This method adds the parameters to the request builder
	 *
	 * @param requestBuilder the request builder to which the parameters should be added
	 * @param params the parameters that should be added
	 * @param session the session of the current scenario
	 */
	private def configureParams(requestBuilder: RequestBuilder, session: Session) {

		if (!paramsAttributes.params.isEmpty) {
			val paramsMap = httpParamsToFluentMap(paramsAttributes.params, session)
			requestBuilder.setParameters(paramsMap)
		}
	}

	private def configureBodyParts(requestBuilder: RequestBuilder, uploadedFiles: List[UploadedFile], session: Session) {
		uploadedFiles.foreach { file =>
			val filePart = file.filePart(session)
			requestBuilder.addBodyPart(filePart)
		}
	}

	private def configureStringParts(requestBuilder: RequestBuilder, session: Session) {
		paramsAttributes.params
			.foreach {
				case (key, values) => values(session).foreach(value => requestBuilder.addBodyPart(new StringPart(key(session), value)))
			}
	}
}