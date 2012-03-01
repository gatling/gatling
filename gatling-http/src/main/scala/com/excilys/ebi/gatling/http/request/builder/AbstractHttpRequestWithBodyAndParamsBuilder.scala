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
import java.io.File
import scala.tools.nsc.io.Path.string2path
import com.excilys.ebi.gatling.core.Predef.stringToSessionFunction
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.PathHelper.path2string
import com.excilys.ebi.gatling.core.util.StringHelper.{ EL_START, EL_END }
import com.excilys.ebi.gatling.http.Predef.{ MULTIPART_FORM_DATA, CONTENT_TYPE, APPLICATION_OCTET_STREAM }
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.ning.http.client.RequestBuilder
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.ning.http.client.FluentStringsMap
import com.ning.http.client.StringPart

/**
 * This class serves as model to HTTP request with a body and parameters
 *
 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
 * @param urlFunction the function returning the url
 * @param queryParams the query parameters that should be added to the request
 * @param params the parameters that should be added to the request
 * @param headers the headers that should be added to the request
 * @param body the body that should be added to the request
 * @param credentials sets the credentials in case of Basic HTTP Authentication
 */
abstract class AbstractHttpRequestWithBodyAndParamsBuilder[B <: AbstractHttpRequestWithBodyAndParamsBuilder[B]](
	requestName: String,
	method: String,
	urlFunction: EvaluatableString,
	queryParams: List[HttpParam],
	params: List[HttpParam],
	headers: Map[String, EvaluatableString],
	body: Option[HttpRequestBody],
	fileUpload: Option[UploadedFile],
	credentials: Option[Credentials],
	checks: Option[List[HttpCheck]])
		extends AbstractHttpRequestWithBodyBuilder[B](requestName, method, urlFunction, queryParams, headers, body, credentials, checks) {

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
	 * @param urlFunction the function returning the url
	 * @param queryParams the query parameters that should be added to the request
	 * @param params the parameters that should be added to the request
	 * @param headers the headers that should be added to the request
	 * @param body the body that should be added to the request
	 * @param credentials sets the credentials in case of Basic HTTP Authentication
	 */
	private[http] def newInstance(
		requestName: String,
		urlFunction: EvaluatableString,
		queryParams: List[HttpParam],
		params: List[HttpParam],
		headers: Map[String, EvaluatableString],
		body: Option[HttpRequestBody],
		fileUpload: Option[UploadedFile],
		credentials: Option[Credentials],
		checks: Option[List[HttpCheck]]): B

	private[http] def newInstance(
		requestName: String,
		urlFunction: EvaluatableString,
		queryParams: List[HttpParam],
		headers: Map[String, EvaluatableString],
		body: Option[HttpRequestBody],
		credentials: Option[Credentials],
		checks: Option[List[HttpCheck]]): B = {
		newInstance(requestName, urlFunction, queryParams, params, headers, body, fileUpload, credentials, checks)
	}

	protected override def getAHCRequestBuilder(session: Session, protocolConfiguration: Option[HttpProtocolConfiguration]): RequestBuilder = {
		val requestBuilder = super.getAHCRequestBuilder(session, protocolConfiguration)
		fileUpload match {
			case Some(fileName) =>
				configureStringParts(requestBuilder, session)
				configureBodyPart(requestBuilder)
			case None => configureParams(requestBuilder, session)
		}

		requestBuilder
	}

	/**
	 *
	 */
	def param(paramKeyFunction: EvaluatableString, paramValueFunction: EvaluatableString): B =
		newInstance(requestName, urlFunction, queryParams, (paramKeyFunction, paramValueFunction) :: params, headers, body, fileUpload, credentials, checks)

	def param(paramKey: String): B = param(paramKey, EL_START + paramKey + EL_END)

	def upload(fileName: String, mimeType: String = APPLICATION_OCTET_STREAM, charset: String = configuration.encoding): B =
		header(CONTENT_TYPE, MULTIPART_FORM_DATA)
			.newInstance(requestName, urlFunction, queryParams, params, headers, body, Some(UploadedFile(fileName, mimeType, charset)), credentials, checks)

	/**
	 * This method adds the parameters to the request builder
	 *
	 * @param requestBuilder the request builder to which the parameters should be added
	 * @param params the parameters that should be added
	 * @param session the session of the current scenario
	 */
	private def configureParams(requestBuilder: RequestBuilder, session: Session) {
		val paramsMap = new FluentStringsMap

		val resolvedParams = for ((keyFunction, valueFunction) <- params) yield (keyFunction(session), valueFunction(session))

		resolvedParams.groupBy(_._1).foreach { entry =>
			val (key, params) = entry
			paramsMap.add(key, params.map(_._2): _*)
		}

		if (!paramsMap.isEmpty) // AHC removes body if setParameters is called
			requestBuilder.setParameters(paramsMap)
	}

	private def configureBodyPart(requestBuilder: RequestBuilder) {
		fileUpload.map(file => requestBuilder.addBodyPart(file.toFilePart))
	}

	private def configureStringParts(requestBuilder: RequestBuilder, session: Session) {
		params.foreach { entry =>
			val key = entry._1(session)
			val value = entry._2(session)
			requestBuilder.addBodyPart(new StringPart(key, value))
		}
	}
}