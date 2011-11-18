/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.request.builder

import java.io.File

import org.fusesource.scalate.Binding
import org.fusesource.scalate.TemplateEngine

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.SavedValue
import com.excilys.ebi.gatling.core.util.FileHelper.SSP_EXTENSION
import com.excilys.ebi.gatling.core.util.PathHelper.GATLING_REQUEST_BODIES_FOLDER
import com.excilys.ebi.gatling.core.util.PathHelper.GATLING_TEMPLATES_FOLDER
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.request.ContextParam
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.TemplateBody
import com.ning.http.client.RequestBuilder

/**
 * This class serves as model to HTTP request with a body
 *
 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
 * @param urlFunction the function returning the url
 * @param queryParams the query parameters that should be added to the request
 * @param headers the headers that should be added to the request
 * @param body the body that should be added to the request
 * @param followsRedirects sets the follow redirect option of AHC
 * @param credentials sets the credentials in case of Basic HTTP Authentication
 */
abstract class AbstractHttpRequestWithBodyBuilder[B <: AbstractHttpRequestWithBodyBuilder[B]](httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Option[Context => String],
	queryParams: List[(Context => String, Context => Option[String])], headers: Map[String, String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)])
		extends AbstractHttpRequestBuilder[B](httpRequestActionBuilder, urlFunction, queryParams, headers, followsRedirects, credentials) {

	override def getRequestBuilder(context: Context): RequestBuilder = {
		val requestBuilder = super.getRequestBuilder(context)
		logger.debug("building in with body")
		requestBuilder setMethod getMethod
		addBodyTo(requestBuilder, body, context)
		requestBuilder
	}

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
	 * @param urlFunction the function returning the url
	 * @param queryParams the query parameters that should be added to the request
	 * @param headers the headers that should be added to the request
	 * @param body the body that should be added to the request
	 * @param followsRedirects sets the follow redirect option of AHC
	 * @param credentials sets the credentials in case of Basic HTTP Authentication
	 */
	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Option[Context => String], queryParams: List[(Context => String, Context => Option[String])], headers: Map[String, String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)]): B

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Option[Context => String], queryParams: List[(Context => String, Context => Option[String])], headers: Map[String, String], followsRedirects: Option[Boolean], credentials: Option[(String, String)]): B = {
		newInstance(httpRequestActionBuilder, urlFunction, queryParams, headers, body, followsRedirects, credentials)
	}

	/**
	 * Adds a body from a file to the request
	 *
	 * @param filePath the path of the file relative to GATLING_REQUEST_BODIES_FOLDER
	 */
	def withFile(filePath: String): B = newInstance(httpRequestActionBuilder, urlFunction, queryParams, headers, Some(FilePathBody(filePath)), followsRedirects, credentials)

	/**
	 * Adds a body to the request
	 *
	 * @param body a string containing the body of the request
	 */
	def withBody(body: String): B = newInstance(httpRequestActionBuilder, urlFunction, queryParams, headers, Some(StringBody(body)), followsRedirects, credentials)

	/**
	 * Adds a body from a template that has to be compiled
	 *
	 * @param tplPath the path to the template relative to GATLING_TEMPLATES_FOLDER
	 * @param values the values that should be merged into the template
	 */
	def withTemplateBody(tplPath: String, values: Map[String, Any]): B = {
		val encapsulatedValues: Map[String, Param] = values.map {
			value =>
				(value._1, value._2 match {
					case SavedValue(s) => ContextParam(s)
					case s => StringParam(s.toString)
				})
		}
		newInstance(httpRequestActionBuilder, urlFunction, queryParams, headers, Some(TemplateBody(tplPath, encapsulatedValues)), followsRedirects, credentials)
	}

	/**
	 * This method adds the body to the request builder
	 *
	 * @param requestBuilder the request builder to which the body should be added
	 * @param body the body that should be added
	 * @param context the context of the current scenario
	 */
	private def addBodyTo(requestBuilder: RequestBuilder, body: Option[HttpRequestBody], context: Context) = {
		body match {
			case Some(thing) =>
				thing match {
					case FilePathBody(filePath) => requestBuilder setBody new File(GATLING_REQUEST_BODIES_FOLDER + "/" + filePath)
					case StringBody(body) => requestBuilder setBody body
					case TemplateBody(tplPath, values) => requestBuilder setBody compileBody(tplPath, values, context)
					case _ =>
				}
			case None =>
		}
	}

	/**
	 * This method compiles the template for a TemplateBody
	 *
	 * @param tplPath the path to the template relative to GATLING_TEMPLATES_FOLDER
	 * @param values the values that should be merged into the template
	 * @param context the context of the current scenario
	 */
	private def compileBody(tplPath: String, values: Map[String, Param], context: Context): String = {

		val engine = new TemplateEngine
		engine.allowCaching = false

		var bindings: List[Binding] = List()
		var templateValues: Map[String, String] = Map.empty

		for (value <- values) {
			bindings = Binding(value._1, "String") :: bindings
			templateValues = templateValues + (value._1 -> (value._2 match {
				case StringParam(string) => string
				case ContextParam(string) => context.getAttribute(string).toString
			}))
		}

		engine.bindings = bindings
		engine.layout(GATLING_TEMPLATES_FOLDER + "/" + tplPath + SSP_EXTENSION, templateValues)
	}
}