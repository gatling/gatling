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

import java.io.{ BufferedInputStream, ByteArrayInputStream, ByteArrayOutputStream, PrintWriter, StringWriter }

import org.fusesource.scalate.{ Binding, DefaultRenderContext, TemplateEngine }

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.action.system
import io.gatling.core.config.GatlingFiles
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

object SspTemplateBodies extends Logging {

	val sessionExtraBinding = Seq(Binding("session", classOf[Session].getName))

	val sspTemplateEngine = {
		val engine = new TemplateEngine(List(GatlingFiles.requestBodiesDirectory.jfile))
		engine.allowReload = false
		engine.escapeMarkup = false
		system.registerOnTermination(engine.shutdown)
		engine
	}

	def buildExpression[T](filePath: Expression[String], additionalAttributes: Map[String, Any], pw: PrintWriter, f: => T): Expression[T] = {

		def sspTemplate(filePath: String): Validation[String] = {
			val file = GatlingFiles.requestBodiesDirectory / filePath
			if (file.exists) filePath.success
			else s"Ssp body file $file doesn't exist".failure
		}

		def layout(templatePath: String, session: Session, additionalAttributes: Map[String, Any], pw: PrintWriter): Validation[PrintWriter] = {

			try {
				withCloseable(pw) { pw =>
					val renderContext = new DefaultRenderContext(templatePath, sspTemplateEngine, pw)
					renderContext.attributes("session") = session
					for ((key, value) <- additionalAttributes) { renderContext.attributes(key) = value }

					sspTemplateEngine.layout(templatePath, renderContext, sessionExtraBinding)
					pw.success
				}

			} catch {
				case e: Exception =>
					logger.warn("Ssp template layout failed", e)
					s"Ssp template layout failed: ${e.getMessage}".failure
			}
		}

		(session: Session) => for {
			path <- filePath(session)
			templatePath <- sspTemplate(path)
			body <- layout(templatePath, session, additionalAttributes, pw)
		} yield f
	}

	def asString(filePath: Expression[String], additionalAttributes: Map[String, Any]): StringBody = {
		val sw = new StringWriter
		val expression = buildExpression(filePath, additionalAttributes, new PrintWriter(sw), sw.toString)
		new StringBody(expression)
	}

	def asBytes(filePath: Expression[String], additionalAttributes: Map[String, Any]): ByteArrayBody = {
		val os = new ByteArrayOutputStream
		val expression = buildExpression(filePath, additionalAttributes, new PrintWriter(os), os.toByteArray)
		new ByteArrayBody(expression)
	}

	def asStream(filePath: Expression[String], additionalAttributes: Map[String, Any]): InputStreamBody = {
		val os = new ByteArrayOutputStream
		val expression = buildExpression(filePath, additionalAttributes, new PrintWriter(os), new ByteArrayInputStream(os.toByteArray))
		new InputStreamBody(expression)
	}
}