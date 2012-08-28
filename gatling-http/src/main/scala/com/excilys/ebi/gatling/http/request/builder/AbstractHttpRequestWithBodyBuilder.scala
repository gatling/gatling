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

import org.fusesource.scalate.{ Binding, TemplateEngine }
import org.fusesource.scalate.support.ScalaCompiler

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.GatlingFiles
import com.excilys.ebi.gatling.core.session.{ EvaluatableString, Session }
import com.excilys.ebi.gatling.core.util.FileHelper.SSP_EXTENSION
import com.excilys.ebi.gatling.core.util.PathHelper.path2jfile
import com.excilys.ebi.gatling.core.util.StringHelper.parseEvaluatable
import com.excilys.ebi.gatling.http.Headers.Names.CONTENT_LENGTH
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.request.{ ByteArrayBody, FilePathBody, HttpRequestBody, SessionByteArrayBody, StringBody, TemplateBody }
import com.ning.http.client.RequestBuilder

object AbstractHttpRequestWithBodyBuilder {
	val TEMPLATE_ENGINE = new TemplateEngine(List(GatlingFiles.requestBodiesDirectory))
	TEMPLATE_ENGINE.allowReload = false
	TEMPLATE_ENGINE.escapeMarkup = false
	// Register engine shutdown
	system.registerOnTermination(TEMPLATE_ENGINE.compiler.asInstanceOf[ScalaCompiler].compiler.askShutdown)
}

/**
 * This class serves as model to HTTP request with a body
 *
 * @param httpAttributes the base HTTP attributes
 * @param body the body that should be added to the request
 */
abstract class AbstractHttpRequestWithBodyBuilder[B <: AbstractHttpRequestWithBodyBuilder[B]](
	httpAttributes: HttpAttributes,
	body: Option[HttpRequestBody])
		extends AbstractHttpRequestBuilder[B](httpAttributes) {

	protected override def getAHCRequestBuilder(session: Session, protocolConfiguration: HttpProtocolConfiguration): RequestBuilder = {
		val requestBuilder = super.getAHCRequestBuilder(session, protocolConfiguration)
		configureBody(requestBuilder, body, session)
		requestBuilder
	}

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param httpAttributes the base HTTP attributes
	 * @param body the body that should be added to the request
	 */
	private[http] def newInstance(
		httpAttributes: HttpAttributes,
		body: Option[HttpRequestBody]): B

	private[http] def newInstance(httpAttributes: HttpAttributes): B = newInstance(httpAttributes, body)

	/**
	 * Adds a body to the request
	 *
	 * @param body a string containing the body of the request
	 */
	def body(body: EvaluatableString): B = newInstance(httpAttributes, Some(StringBody(body)))

	/**
	 * Adds a body from a file to the request
	 *
	 * @param filePath the path of the file relative to directory containing the templates
	 */
	def fileBody(filePath: String): B = newInstance(httpAttributes, Some(FilePathBody(filePath)))

	/**
	 * Adds a body from a template that has to be compiled
	 *
	 * @param tplPath the path to the template relative to GATLING_TEMPLATES_FOLDER
	 * @param values the values that should be merged into the template
	 */
	def fileBody(tplPath: String, values: Map[String, String]): B = {
		val evaluatableValues = values.map { entry => entry._1 -> parseEvaluatable(entry._2) }
		newInstance(httpAttributes, Some(TemplateBody(tplPath, evaluatableValues)))
	}

	/**
	 * Adds a body from a byteArray to the request
	 *
	 * @param byteArray - The callback function which returns the ByteArray from which to build the body
	 */
	def byteArrayBody(byteArray: () => Array[Byte]): B = newInstance(httpAttributes, Some(ByteArrayBody(byteArray)))

	/**
	 * Adds a body from a byteArray Session function to the request
	 *
	 * @param byteArray - The callback function which returns the ByteArray from which to build the body
	 */
	def byteArrayBody(byteArray: (Session) => Array[Byte]): B = newInstance(httpAttributes, Some(SessionByteArrayBody(byteArray)))

	/**
	 * This method adds the body to the request builder
	 *
	 * @param requestBuilder the request builder to which the body should be added
	 * @param body the body that should be added
	 * @param session the session of the current scenario
	 */
	private def configureBody(requestBuilder: RequestBuilder, body: Option[HttpRequestBody], session: Session) {

		body match {
			case Some(FilePathBody(filePath)) =>
				requestBuilder.setBody((GatlingFiles.requestBodiesDirectory / filePath).jfile)

			case Some(StringBody(body)) =>
				requestBuilder.setBody(body(session))

			case Some(TemplateBody(tplPath, values)) =>
				requestBuilder.setBody(compileBody(tplPath, values, session))

			case Some(ByteArrayBody(byteArray)) =>
				val body = byteArray()
				requestBuilder.setBody(body)
				requestBuilder.setHeader(CONTENT_LENGTH, body.length.toString)

			case Some(SessionByteArrayBody(byteArray)) =>
				val body = byteArray(session)
				requestBuilder.setBody(body)
				requestBuilder.setHeader(CONTENT_LENGTH, body.length.toString)

			case None =>
		}
	}

	/**
	 * This method compiles the template for a TemplateBody
	 *
	 * @param tplPath the path to the template relative to GATLING_TEMPLATES_FOLDER
	 * @param params the params that should be merged into the template
	 * @param session the session of the current scenario
	 */
	private def compileBody(tplPath: String, params: Map[String, EvaluatableString], session: Session): String = {

		val bindings = for ((key, _) <- params) yield Binding(key, "String")
		val templateValues = for ((key, value) <- params) yield (key -> (value(session)))

		AbstractHttpRequestWithBodyBuilder.TEMPLATE_ENGINE.layout(tplPath + SSP_EXTENSION, templateValues, bindings)
	}
}
