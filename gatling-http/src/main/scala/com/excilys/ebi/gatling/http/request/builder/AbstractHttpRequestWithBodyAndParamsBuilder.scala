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
import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_ENCODING
import com.excilys.ebi.gatling.core.config.GatlingFiles.GATLING_REQUEST_BODIES_FOLDER
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.PathHelper.path2string
import com.excilys.ebi.gatling.core.util.StringHelper.{ EL_START, EL_END }
import com.excilys.ebi.gatling.http.Predef.{ MULTIPART_FORM_DATA, CONTENT_TYPE, APPLICATION_OCTET_STREAM }
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.ning.http.client.{ StringPart, RequestBuilder, FluentStringsMap, FilePart }

/**
 * This class serves as model to HTTP request with a body and parameters
 *
 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
 * @param urlFunction the function returning the url
 * @param queryParams the query parameters that should be added to the request
 * @param params the parameters that should be added to the request
 * @param headers the headers that should be added to the request
 * @param body the body that should be added to the request
 * @param followsRedirects sets the follow redirect option of AHC
 * @param credentials sets the credentials in case of Basic HTTP Authentication
 */
abstract class AbstractHttpRequestWithBodyAndParamsBuilder[B <: AbstractHttpRequestWithBodyAndParamsBuilder[B]](httpRequestActionBuilder: HttpRequestActionBuilder, method: String,
	urlFunction: Session => String, queryParams: List[(Session => String, Session => String)], params: List[(Session => String, Session => String)], headers: Map[String, Session => String],
	body: Option[HttpRequestBody], fileUpload: Option[(String, String, String)], followsRedirects: Option[Boolean], credentials: Option[(String, String)])
		extends AbstractHttpRequestWithBodyBuilder[B](httpRequestActionBuilder, method, urlFunction, queryParams, headers, body, followsRedirects, credentials) {

	private[http] override def getRequestBuilder(session: Session, protocolConfiguration: Option[HttpProtocolConfiguration]): RequestBuilder = {
		val requestBuilder = super.getRequestBuilder(session, protocolConfiguration)
		fileUpload.map { fileName =>
			addStringPartsTo(requestBuilder, session)
			addBodyPartTo(requestBuilder)
		}.getOrElse {
			addParamsTo(requestBuilder, session)
		}
		requestBuilder
	}

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
	 * @param urlFunction the function returning the url
	 * @param queryParams the query parameters that should be added to the request
	 * @param params the parameters that should be added to the request
	 * @param headers the headers that should be added to the request
	 * @param body the body that should be added to the request
	 * @param followsRedirects sets the follow redirect option of AHC
	 * @param credentials sets the credentials in case of Basic HTTP Authentication
	 */
	private[http] def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Session => String, queryParams: List[(Session => String, Session => String)], params: List[(Session => String, Session => String)], headers: Map[String, Session => String], body: Option[HttpRequestBody], fileUpload: Option[(String, String, String)], followsRedirects: Option[Boolean], credentials: Option[(String, String)]): B

	private[http] def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Session => String, queryParams: List[(Session => String, Session => String)], headers: Map[String, Session => String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)]): B = {
		newInstance(httpRequestActionBuilder, urlFunction, queryParams, params, headers, body, fileUpload, followsRedirects, credentials)
	}

	/**
	 *
	 */
	def param(paramKeyFunction: Session => String, paramValueFunction: Session => String): B =
		newInstance(httpRequestActionBuilder, urlFunction, queryParams, (paramKeyFunction, paramValueFunction) :: params, headers, body, fileUpload, followsRedirects, credentials)

	def param(paramKey: String): B = param(paramKey, EL_START + paramKey + EL_END)

	def fileUpload(fileName: String, mimeType: String = APPLICATION_OCTET_STREAM, charset: String = CONFIG_ENCODING): B =
		header(CONTENT_TYPE, MULTIPART_FORM_DATA)
			.newInstance(httpRequestActionBuilder, urlFunction, queryParams, params, headers, body, Some((fileName, mimeType, charset)), followsRedirects, credentials)

	/**
	 * This method adds the parameters to the request builder
	 *
	 * @param requestBuilder the request builder to which the parameters should be added
	 * @param params the parameters that should be added
	 * @param session the session of the current scenario
	 */
	private def addParamsTo(requestBuilder: RequestBuilder, session: Session) = {
		val paramsMap = new FluentStringsMap

		val keyValues = for ((keyFunction, valueFunction) <- params) yield (keyFunction(session), valueFunction(session))

		keyValues.groupBy(_._1).foreach { entry =>
			val (key, values) = entry
			paramsMap.add(key, values.map(_._2): _*)
		}

		if (!paramsMap.isEmpty) // AHC removes body if setParameters is called
			requestBuilder setParameters paramsMap
	}

	private def addBodyPartTo(requestBuilder: RequestBuilder) = {
		val (fileName, mimeType, charset) = fileUpload.get
		requestBuilder.addBodyPart(new FilePart(fileName, new File(GATLING_REQUEST_BODIES_FOLDER / fileName), mimeType, charset))
	}

	private def addStringPartsTo(requestBuilder: RequestBuilder, session: Session) = {
		params.foreach { entry =>
			val (key, value) = (entry._1(session), entry._2(session))
			requestBuilder.addBodyPart(new StringPart(key, value))
		}

	}
}