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

import org.fusesource.scalate.{ TemplateEngine, Binding }

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.FileHelper.SSP_EXTENSION
import com.excilys.ebi.gatling.core.util.PathHelper.{ GATLING_TEMPLATES_FOLDER, GATLING_REQUEST_BODIES_FOLDER }
import com.excilys.ebi.gatling.core.util.StringHelper.interpolate
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.request.{ TemplateBody, StringBody, HttpRequestBody, FilePathBody }
import com.ning.http.client.RequestBuilder

object AbstractHttpRequestWithBodyBuilder {
	val engine = new TemplateEngine(List(new File(GATLING_TEMPLATES_FOLDER)))
	engine.allowReload = false
	engine.escapeMarkup = false
}

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
abstract class AbstractHttpRequestWithBodyBuilder[B <: AbstractHttpRequestWithBodyBuilder[B]](httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Context => String,
	queryParams: List[(Context => String, Context => String)], headers: Map[String, String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)])
		extends AbstractHttpRequestBuilder[B](httpRequestActionBuilder, urlFunction, queryParams, headers, followsRedirects, credentials) {

	override def getRequestBuilder(context: Context): RequestBuilder = {
		val requestBuilder = super.getRequestBuilder(context)
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
	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Context => String, queryParams: List[(Context => String, Context => String)], headers: Map[String, String], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[(String, String)]): B

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFunction: Context => String, queryParams: List[(Context => String, Context => String)], headers: Map[String, String], followsRedirects: Option[Boolean], credentials: Option[(String, String)]): B = {
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
	def withTemplateBody(tplPath: String, values: Map[String, String]): B = {
		val interpolatedValues = values.map { entry => entry._1 -> interpolate(entry._2) }
		newInstance(httpRequestActionBuilder, urlFunction, queryParams, headers, Some(TemplateBody(tplPath, interpolatedValues)), followsRedirects, credentials)
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
	private def compileBody(tplPath: String, values: Map[String, Context => String], context: Context): String = {

		val bindings = for (value <- values) yield Binding(value._1, "String")
		val templateValues = for (value <- values) yield (value._1 -> (value._2(context)))

		AbstractHttpRequestWithBodyBuilder.engine.layout(tplPath + SSP_EXTENSION, templateValues, bindings)
	}
}