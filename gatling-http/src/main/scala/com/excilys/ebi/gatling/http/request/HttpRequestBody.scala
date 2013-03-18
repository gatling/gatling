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
import com.excilys.ebi.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation, ValidationList }
import com.ning.http.client.RequestBuilder

object HttpRequestBody {

	val elFileCache = new collection.mutable.HashMap[String, Validation[Expression[String]]]

	private def template(filePath: String): Validation[Path] = {
		val file = GatlingFiles.requestBodiesDirectory / filePath
		if (file.exists) file.success
		else s"Body file $file doesn't exist".failure
	}

	def elTemplateBody(filePath: Expression[String]): StringBody = {

		def compileTemplate(path: String): Validation[Expression[String]] =
			template(path)
				.map(f => FileUtils.readFileToString(f.jfile, GatlingConfiguration.configuration.simulation.encoding))
				.map(EL.compile[String])

		def fetchTemplate(path: String): Validation[Expression[String]] = elFileCache.getOrElseUpdate(path, compileTemplate(path))

		val expression = (session: Session) => {
			for {
				path <- filePath(session)
				expression <- fetchTemplate(path)
				body <- expression(session)
			} yield body
		}

		StringBody(expression)
	}

	def sspTemplateBody(filePath: Expression[String], additionalAttributes: Map[String, Any]): SspTemplateBody = {

		def sspTemplate(filePath: String): Validation[String] = {
			val file = GatlingFiles.requestBodiesDirectory / filePath
			if (file.exists) filePath.success
			else s"Ssp body file $file doesn't exist".failure
		}

		val templatePathExpression = (session: Session) =>
			for {
				path <- filePath(session)
				ssp <- sspTemplate(path)
			} yield ssp

		SspTemplateBody(templatePathExpression, additionalAttributes)
	}

	def rawFileBody(filePath: Expression[String]) = {

		val expression = (session: Session) =>
			for {
				path <- filePath(session)
				file <- template(path)
			} yield file.jfile

		RawFileBody(expression)
	}
	
	val sessionExtraBinding = Seq(Binding("session", classOf[Session].getName))

	val sspTemplateEngine = {
		val engine = new TemplateEngine(List(GatlingFiles.requestBodiesDirectory.jfile))
		engine.allowReload = false
		engine.escapeMarkup = false
		system.registerOnTermination(engine.shutdown)
		engine
	}
}

sealed trait HttpRequestBody {

	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder]
}
case class StringBody(expression: Expression[String]) extends HttpRequestBody {

	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] =
		expression(session).map(requestBuilder.setBody)
}
case class RawFileBody(file: Expression[JFile]) extends HttpRequestBody {

	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] =
		file(session).map(requestBuilder.setBody)
}
case class SspTemplateBody(templatePathExpression: Expression[String], additionalAttributes: Map[String, Any]) extends HttpRequestBody {

	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] =
		for {
			templatePath <- templatePathExpression(session)
			body = HttpRequestBody.sspTemplateEngine.layout(templatePath, additionalAttributes + ("session" -> session), HttpRequestBody.sessionExtraBinding)
		} yield requestBuilder.setBody(body)
}
case class ByteArrayBody(byteArray: Expression[Array[Byte]]) extends HttpRequestBody {

	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] =
		byteArray(session).map(requestBuilder.setBody)
}

