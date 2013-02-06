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
package com.excilys.ebi.gatling.http.request

import java.io.{ File => JFile }

import scala.reflect.io.Path
import scala.reflect.io.Path.string2path

import org.apache.commons.io.FileUtils
import org.fusesource.scalate.{ Binding, TemplateEngine }

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.{ GatlingConfiguration, GatlingFiles }
import com.excilys.ebi.gatling.core.session.{ EL, Expression, Session }
import com.excilys.ebi.gatling.core.util.FlattenableValidations
import com.ning.http.client.RequestBuilder

import scalaz.Scalaz.ToValidationV
import scalaz.Validation

object HttpRequestBody {

	val EL_FILE_CACHE = new collection.mutable.HashMap[String, Expression[String]]

	private def template(filePath: String): Path = {
		val file = GatlingFiles.requestBodiesDirectory / filePath
		require(file.exists, s"Raw body file $file doesn't exist")
		file
	}

	def compileELTemplateBody(filePath: String): StringBody = {

		def compile() = {

			val file = template(filePath)
			val fileContent = FileUtils.readFileToString(file.jfile, GatlingConfiguration.configuration.simulation.encoding)
			EL.compile[String](fileContent)
		}

		val expression = EL_FILE_CACHE.getOrElseUpdate(filePath, compile())
		new StringBody(expression)
	}

	def compileSspTemplateBody(filePath: String, params: Map[String, String]): SspTemplateBody = {

		template(filePath)

		val attributesExpression = (session: Session) => params
			.map {
				case (key, value) =>
					val expression = EL.compile[String](value)
					expression(session).map(key -> _)
			}.toList
			.flattenIt
			.map(_.toMap)

		val bindings = params.keySet.map(Binding(_, "String"))

		new SspTemplateBody(filePath, attributesExpression, bindings)
	}

	def compileRawFileBody(filePath: String) = {
		val file = template(filePath)
		new RawFileBody(file.jfile)
	}

	val SSP_TEMPLATE_ENGINE = {
		val engine = new TemplateEngine(List(GatlingFiles.requestBodiesDirectory.jfile))
		engine.allowReload = false
		engine.escapeMarkup = false
		system.registerOnTermination(engine.shutdown)
		engine
	}
}

sealed trait HttpRequestBody {

	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[String, RequestBuilder]
}
class StringBody(expression: Expression[String]) extends HttpRequestBody {

	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[String, RequestBuilder] =
		expression(session).map(string => requestBuilder.setBody(string).setContentLength(string.length))
}
class RawFileBody(file: JFile) extends HttpRequestBody {

	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[String, RequestBuilder] =
		requestBuilder.setBody(file).setContentLength(file.length.toInt).success
}
class SspTemplateBody(tplPath: String, attributesExpression: Expression[Map[String, String]], bindings: Traversable[Binding]) extends HttpRequestBody {

	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[String, RequestBuilder] =
		attributesExpression(session)
			.map(HttpRequestBody.SSP_TEMPLATE_ENGINE.layout(tplPath, _, bindings))
			.map(string => requestBuilder.setBody(string).setContentLength(string.length))
}
class SessionByteArrayBody(byteArray: Session => Array[Byte]) extends HttpRequestBody {

	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[String, RequestBuilder] = {
		val bytes = byteArray(session)
		requestBuilder.setBody(bytes).setContentLength(bytes.length).success
	}
}

