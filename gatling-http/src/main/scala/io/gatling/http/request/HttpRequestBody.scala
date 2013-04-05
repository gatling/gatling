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
package io.gatling.http.request

import java.io.{ ByteArrayOutputStream, File => JFile, InputStream, PrintWriter }

import scala.collection.mutable

import org.apache.commons.io.FileUtils
import org.fusesource.scalate.{ Binding, TemplateEngine }

import com.ning.http.client.RequestBuilder
import com.ning.http.client.generators.InputStreamBodyGenerator
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.action.system
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles
import io.gatling.core.session.{ EL, Expression, Session }
import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

object StringBodies {

	def build(expression: Expression[String]): ByteArrayBody = {
		val bytes = (session: Session) => expression(session).map(_.getBytes(configuration.simulation.encoding))
		new ByteArrayBody(bytes)
	}
}

object ELTemplateBodies {

	val elTemplateBodiesCache = new collection.mutable.HashMap[String, Validation[Expression[String]]]

	def build(filePath: Expression[String]): ByteArrayBody = {

		def compileTemplate(path: String): Validation[Expression[String]] =
			GatlingFiles.requestBodyFile(path)
				.map(f => FileUtils.readFileToString(f.jfile, configuration.simulation.encoding))
				.map(EL.compile[String])

		val bytes = (session: Session) =>
			for {
				path <- filePath(session)
				expression <- elTemplateBodiesCache.getOrElseUpdate(path, compileTemplate(path))
				body <- expression(session)
			} yield body.getBytes(configuration.simulation.encoding)

		new ByteArrayBody(bytes)
	}
}

object SspTemplateBodies extends Logging {

	val sessionExtraBinding = Seq(Binding("session", classOf[Session].getName))

	val sspTemplateEngine = {
		val engine = new TemplateEngine(List(GatlingFiles.requestBodiesDirectory.jfile))
		engine.allowReload = false
		engine.escapeMarkup = false
		system.registerOnTermination(engine.shutdown)
		engine
	}

	def build(filePath: Expression[String], additionalAttributes: Map[String, Any]): ByteArrayBody = {

		def sspTemplate(filePath: String): Validation[String] = {
			val file = GatlingFiles.requestBodiesDirectory / filePath
			if (file.exists) filePath.success
			else s"Ssp body file $file doesn't exist".failure
		}

		def layout(templatePath: String, session: Session): Validation[Array[Byte]] = {
			val out = new ByteArrayOutputStream
			try {
				withCloseable(new PrintWriter(out)) { pw =>
					sspTemplateEngine.layout(templatePath, additionalAttributes + ("session" -> session), sessionExtraBinding)
				}
				out.toByteArray.success
			} catch {
				case e: Exception =>
					logger.warn("Ssp template layout failed", e)
					s"Ssp template layout failed: ${e.getMessage}".failure
			}
		}

		val expression = (session: Session) =>

			for {
				path <- filePath(session)
				templatePath <- sspTemplate(path)
				body <- layout(templatePath, session)
			} yield body

		new ByteArrayBody(expression)
	}
}

object RawFileBodies {

	def build(filePath: Expression[String]): RawFileBody = {

		val expression = (session: Session) =>
			for {
				path <- filePath(session)
				file <- GatlingFiles.requestBodyFile(path)
			} yield file.jfile

		new RawFileBody(expression)
	}
}

trait HttpRequestBody {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder]
}

case class RawFileBody(val file: Expression[JFile]) extends HttpRequestBody {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = file(session).map(requestBuilder.setBody)
}

case class ByteArrayBody(val byteArray: Expression[Array[Byte]]) extends HttpRequestBody {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = byteArray(session).map(requestBuilder.setBody)
}

case class InputStreamBody(val is: Expression[InputStream]) extends HttpRequestBody {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = is(session).map(is => requestBuilder.setBody(new InputStreamBodyGenerator(is)))
}
