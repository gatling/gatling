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

import scala.reflect.io.Path

import com.ning.http.client.RequestBuilder

import io.gatling.core.action.system
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles
import io.gatling.core.session.{ EL, Expression, Session }
import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

object StringBody {

	def apply(expression: Expression[String]): ByteArrayBody = {
		val bytes = (session: Session) => expression(session).map(_.getBytes(configuration.simulation.encoding))
		new ByteArrayBody(bytes)
	}
}

object ELTemplateBody {

	import org.apache.commons.io.FileUtils

	val elFileCache = new collection.mutable.HashMap[String, Validation[Expression[String]]]

	def template(filePath: String): Validation[Path] = {
		val file = GatlingFiles.requestBodiesDirectory / filePath
		if (file.exists) file.success
		else s"Body file $file doesn't exist".failure
	}

	def apply(filePath: Expression[String]): ByteArrayBody = {

		def compileTemplate(path: String): Validation[Expression[String]] =
			template(path)
				.map(f => FileUtils.readFileToString(f.jfile, configuration.simulation.encoding))
				.map(EL.compile[String])

		def fetchTemplate(path: String): Validation[Expression[String]] = elFileCache.getOrElseUpdate(path, compileTemplate(path))

		val bytes = (session: Session) =>
			for {
				path <- filePath(session)
				expression <- fetchTemplate(path)
				body <- expression(session)
			} yield body.getBytes(configuration.simulation.encoding)

		new ByteArrayBody(bytes)
	}
}

sealed trait HttpRequestBody {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder]
}

object SspTemplateBody {

	import org.fusesource.scalate.{ Binding, TemplateEngine }

	val sessionExtraBinding = Seq(Binding("session", classOf[Session].getName))

	val sspTemplateEngine = {
		val engine = new TemplateEngine(List(GatlingFiles.requestBodiesDirectory.jfile))
		engine.allowReload = false
		engine.escapeMarkup = false
		system.registerOnTermination(engine.shutdown)
		engine
	}

	def apply(filePath: Expression[String], additionalAttributes: Map[String, Any]) = {

		def sspTemplate(filePath: String): Validation[String] = {
			val file = GatlingFiles.requestBodiesDirectory / filePath
			if (file.exists) filePath.success
			else s"Ssp body file $file doesn't exist".failure
		}

		def layout(templatePath: String, session: Session): Array[Byte] = {
			val out = new ByteArrayOutputStream
			withCloseable(new PrintWriter(out)) { pw =>
				sspTemplateEngine.layout(templatePath, additionalAttributes + ("session" -> session), sessionExtraBinding)
			}
			out.toByteArray
		}

		val expression = (session: Session) =>
			for {
				path <- filePath(session)
				templatePath <- sspTemplate(path)
				body = layout(templatePath, session)
			} yield body

		new ByteArrayBody(expression)
	}
}

object RawFileBody {

	def apply(filePath: Expression[String]) = {

		val expression = (session: Session) =>
			for {
				path <- filePath(session)
				file <- ELTemplateBody.template(path)
			} yield file.jfile

		new RawFileBody(expression)
	}
}

class RawFileBody(file: Expression[JFile]) extends HttpRequestBody {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = file(session).map(requestBuilder.setBody)
}
class ByteArrayBody(byteArray: Expression[Array[Byte]]) extends HttpRequestBody {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = byteArray(session).map(requestBuilder.setBody)
}
class InputStreamBody(is: Expression[InputStream]) extends HttpRequestBody {

	import com.ning.http.client.generators.InputStreamBodyGenerator

	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = is(session).map(is => requestBuilder.setBody(new InputStreamBodyGenerator(is)))
}
