/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import scala.tools.nsc.io.Path.string2path

import org.apache.commons.io.FileUtils
import org.fusesource.scalate.{ Binding, TemplateEngine }

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.{ GatlingConfiguration, GatlingFiles }
import com.excilys.ebi.gatling.core.session.{ EL, Expression, Session }
import com.excilys.ebi.gatling.core.util.FlattenableValidations
import com.excilys.ebi.gatling.http.Headers.Names.CONTENT_LENGTH
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.request.{ HttpRequestBody, SessionByteArrayBody, StringBody }
import com.ning.http.client.RequestBuilder

import scalaz.Scalaz.ToValidationV
import scalaz.Validation

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
	def body(body: Expression[String]): B = newInstance(httpAttributes, Some(new StringBody(body)))

	/**
	 * Adds a body from a file to the request
	 *
	 * @param filePath the path of the file relative to directory containing the templates
	 */
	def rawFileBody(filePath: Expression[String]): B = newInstance(httpAttributes, Some(HttpRequestBody.compileRawFileBody(filePath)))

	/**
	 * Adds a body from a file to the request
	 *
	 * @param filePath the path of the file relative to directory containing the templates
	 */
	def fileBody(filePath: Expression[String]): B = newInstance(httpAttributes, Some(HttpRequestBody.compileELTemplateBody(filePath)))

	/**
	 * Adds a body from a template that has to be compiled
	 *
	 * @param tplPath the path to the template relative to GATLING_TEMPLATES_FOLDER
	 * @param values the values that should be merged into the template
	 */
	def sspBody(filePath: Expression[String], values: Map[String, String]): B = newInstance(httpAttributes, Some(HttpRequestBody.compileSspTemplateBody(filePath, values)))

	/**
	 * Adds a body from a byteArray Session function to the request
	 *
	 * @param byteArray - The callback function which returns the ByteArray from which to build the body
	 */
	def byteArrayBody(byteArray: (Session) => Array[Byte]): B = newInstance(httpAttributes, Some(new SessionByteArrayBody(byteArray)))

	protected override def getAHCRequestBuilder(session: Session, protocolConfiguration: HttpProtocolConfiguration): Validation[String, RequestBuilder] = {

		val requestBuilder = super.getAHCRequestBuilder(session, protocolConfiguration)

		body match {
			case Some(body) => requestBuilder.flatMap(body.setBody(_, session))
			case _ => requestBuilder
		}
	}
}
