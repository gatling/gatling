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

import scala.tools.nsc.io.Path.string2path
import org.fusesource.scalate.support.ScalaCompiler
import org.fusesource.scalate.{ TemplateEngine, Binding }
import com.excilys.ebi.gatling.core.config.GatlingFiles
import com.excilys.ebi.gatling.core.resource.ResourceRegistry
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.FileHelper.SSP_EXTENSION
import com.excilys.ebi.gatling.core.util.PathHelper.path2jfile
import com.excilys.ebi.gatling.core.util.StringHelper.parseEvaluatable
import com.excilys.ebi.gatling.http.request.{ TemplateBody, StringBody, HttpRequestBody, FilePathBody }
import com.ning.http.client.RequestBuilder
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestWithBodyBuilder.TEMPLATE_ENGINE
import com.ning.http.client.Realm

object AbstractHttpRequestWithBodyBuilder {
	lazy val TEMPLATE_ENGINE = initEngine

	def initEngine: TemplateEngine = {
		val engine = new TemplateEngine(List(GatlingFiles.requestBodiesFolder))
		engine.allowReload = false
		engine.escapeMarkup = false
		// Register engine shutdown
		ResourceRegistry.registerOnCloseCallback(() => engine.compiler.asInstanceOf[ScalaCompiler].compiler.askShutdown)
		engine
	}
}

/**
 * This class serves as model to HTTP request with a body
 *
 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
 * @param url the function returning the url
 * @param queryParams the query parameters that should be added to the request
 * @param headers the headers that should be added to the request
 * @param body the body that should be added to the request
 * @param realm sets the realm in case of Basic HTTP Authentication
 */
abstract class AbstractHttpRequestWithBodyBuilder[B <: AbstractHttpRequestWithBodyBuilder[B]](
	requestName: String,
	method: String,
	url: EvaluatableString,
	queryParams: List[HttpParam],
	headers: Map[String, EvaluatableString],
	body: Option[HttpRequestBody],
	realm: Option[Session => Realm],
	checks: Option[List[HttpCheck]])
		extends AbstractHttpRequestBuilder[B](requestName, method, url, queryParams, headers, realm, checks) {

	protected override def getAHCRequestBuilder(session: Session, protocolConfiguration: Option[HttpProtocolConfiguration]): RequestBuilder = {
		val requestBuilder = super.getAHCRequestBuilder(session, protocolConfiguration)
		configureBody(requestBuilder, body, session)
		requestBuilder
	}

	/**
	 * Method overridden in children to create a new instance of the correct type
	 *
	 * @param httpRequestActionBuilder the HttpRequestActionBuilder with which this builder is linked
	 * @param url the function returning the url
	 * @param queryParams the query parameters that should be added to the request
	 * @param headers the headers that should be added to the request
	 * @param body the body that should be added to the request
	 * @param realm sets the realm in case of Basic HTTP Authentication
	 */
	private[http] def newInstance(
		requestName: String,
		url: EvaluatableString,
		queryParams: List[HttpParam],
		headers: Map[String, EvaluatableString],
		body: Option[HttpRequestBody],
		realm: Option[Session => Realm],
		checks: Option[List[HttpCheck]]): B

	private[http] def newInstance(
		requestName: String,
		url: EvaluatableString,
		queryParams: List[HttpParam],
		headers: Map[String, EvaluatableString],
		realm: Option[Session => Realm],
		checks: Option[List[HttpCheck]]): B = {
		newInstance(requestName, url, queryParams, headers, body, realm, checks)
	}

	/**
	 * Adds a body to the request
	 *
	 * @param body a string containing the body of the request
	 */
	def body(body: EvaluatableString): B = newInstance(requestName, url, queryParams, headers, Some(StringBody(body)), realm, checks)

	/**
	 * Adds a body from a file to the request
	 *
	 * @param filePath the path of the file relative to GATLING_REQUEST_BODIES_FOLDER
	 */
	def fileBody(filePath: String): B = newInstance(requestName, url, queryParams, headers, Some(FilePathBody(filePath)), realm, checks)

	/**
	 * Adds a body from a template that has to be compiled
	 *
	 * @param tplPath the path to the template relative to GATLING_TEMPLATES_FOLDER
	 * @param values the values that should be merged into the template
	 */
	def fileBody(tplPath: String, values: Map[String, String]): B = {
		val evaluatableValues = values.map { entry => entry._1 -> parseEvaluatable(entry._2) }
		newInstance(requestName, url, queryParams, headers, Some(TemplateBody(tplPath, evaluatableValues)), realm, checks)
	}

	/**
	 * This method adds the body to the request builder
	 *
	 * @param requestBuilder the request builder to which the body should be added
	 * @param body the body that should be added
	 * @param session the session of the current scenario
	 */
	private def configureBody(requestBuilder: RequestBuilder, body: Option[HttpRequestBody], session: Session) {
		body match {
			case Some(thing) =>
				thing match {
					case FilePathBody(filePath) => requestBuilder.setBody((GatlingFiles.requestBodiesFolder / filePath).jfile)
					case StringBody(body) => requestBuilder.setBody(body(session))
					case TemplateBody(tplPath, values) => requestBuilder.setBody(compileBody(tplPath, values, session))
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
	 * @param session the session of the current scenario
	 */
	private def compileBody(tplPath: String, values: Map[String, EvaluatableString], session: Session): String = {

		val bindings = for (value <- values) yield Binding(value._1, "String")
		val templateValues = for (value <- values) yield (value._1 -> (value._2(session)))

		TEMPLATE_ENGINE.layout(tplPath + SSP_EXTENSION, templateValues, bindings)
	}
}