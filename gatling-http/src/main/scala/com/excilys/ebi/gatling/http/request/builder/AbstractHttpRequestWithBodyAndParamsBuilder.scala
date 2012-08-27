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

import com.excilys.ebi.gatling.core.Predef.stringToSessionFunction
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.session.{ EvaluatableString, Session }
import com.excilys.ebi.gatling.core.util.StringHelper.{ EL_END, EL_START }
import com.excilys.ebi.gatling.http.Headers.{ Names => HeaderNames, Values => HeaderValues }
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.ning.http.client.{ FluentStringsMap, Realm, RequestBuilder, StringPart }

/**
 * This class serves as model to HTTP request with a body and parameters
 *
 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
 * @param urlFunction the function returning the url
 * @param queryParams the query parameters that should be added to the request
 * @param params the parameters that should be added to the request
 * @param headers the headers that should be added to the request
 * @param body the body that should be added to the request
 * @param realm sets the realm in case of Basic HTTP Authentication
 */
abstract class AbstractHttpRequestWithBodyAndParamsBuilder[B <: AbstractHttpRequestWithBodyAndParamsBuilder[B]](
	requestName: String,
	method: String,
	url: EvaluatableString,
	queryParams: List[HttpParam],
	params: List[HttpParam],
	headers: Map[String, EvaluatableString],
	body: Option[HttpRequestBody],
	uploadedFiles: List[UploadedFile],
	realm: Option[Session => Realm],
	checks: List[HttpCheck[_]])
		extends AbstractHttpRequestWithBodyBuilder[B](requestName, method, url, queryParams, headers, body, realm, checks) {

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
	 * @param urlFunction the function returning the url
	 * @param queryParams the query parameters that should be added to the request
	 * @param params the parameters that should be added to the request
	 * @param headers the headers that should be added to the request
	 * @param body the body that should be added to the request
	 * @param realm sets the realm in case of Basic HTTP Authentication
	 */
	private[http] def newInstance(
		requestName: String,
		url: EvaluatableString,
		queryParams: List[HttpParam],
		params: List[HttpParam],
		headers: Map[String, EvaluatableString],
		body: Option[HttpRequestBody],
		uploadedFiles: List[UploadedFile],
		realm: Option[Session => Realm],
		checks: List[HttpCheck[_]]): B

	private[http] def newInstance(
		requestName: String,
		url: EvaluatableString,
		queryParams: List[HttpParam],
		headers: Map[String, EvaluatableString],
		body: Option[HttpRequestBody],
		realm: Option[Session => Realm],
		checks: List[HttpCheck[_]]): B = {
		newInstance(requestName, url, queryParams, params, headers, body, uploadedFiles, realm, checks)
	}

	protected override def getAHCRequestBuilder(session: Session, protocolConfiguration: HttpProtocolConfiguration): RequestBuilder = {
		val requestBuilder = super.getAHCRequestBuilder(session, protocolConfiguration)
		if (uploadedFiles.isEmpty)
			configureParams(requestBuilder, session)
		else {
			configureStringParts(requestBuilder, session)
			configureBodyParts(requestBuilder, uploadedFiles, session)
		}

		requestBuilder
	}

	def param(key: EvaluatableString, value: EvaluatableString): B =
		newInstance(requestName, url, queryParams, (key, value) :: params, headers, body, uploadedFiles, realm, checks)

	def param(paramKey: String): B = param(paramKey, EL_START + paramKey + EL_END)

	def upload(paramKey: EvaluatableString, fileName: EvaluatableString, mimeType: String = HeaderValues.APPLICATION_OCTET_STREAM, charset: String = configuration.encoding): B =
		header(HeaderNames.CONTENT_TYPE, HeaderValues.MULTIPART_FORM_DATA)
			.newInstance(requestName, url, queryParams, params, headers, body, new UploadedFile(paramKey, fileName, mimeType, charset) :: uploadedFiles, realm, checks)

	/**
	 * This method adds the parameters to the request builder
	 *
	 * @param requestBuilder the request builder to which the parameters should be added
	 * @param params the parameters that should be added
	 * @param session the session of the current scenario
	 */
	private def configureParams(requestBuilder: RequestBuilder, session: Session) {

		if (!params.isEmpty) {
			val paramsMap = new FluentStringsMap

			params
				.map { case (key, value) => (key(session), value(session)) }
				.groupBy(_._1)
				.foreach { case (key, params) => paramsMap.add(key, params.map(_._2)) }

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
		params.foreach {
			case (key, value) => requestBuilder.addBodyPart(new StringPart(key(session), value(session)))
		}
	}
}