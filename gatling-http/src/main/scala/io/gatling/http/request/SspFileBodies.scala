/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.io.{ PrintWriter, StringWriter }

import org.fusesource.scalate.{ Binding, DefaultRenderContext, TemplateEngine }

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.action.system
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

@deprecated("Scalate support will be dropped in 2.1.0, prefer EL files or plain scala code", "2.0.0")
object SspFileBodies extends Logging {

	val sessionExtraBinding = Seq(Binding("session", classOf[Session].getName))

	val sspFileEngine = {
		val engine = new TemplateEngine(List(GatlingFiles.requestBodiesDirectory.jfile))
		engine.allowReload = false
		engine.escapeMarkup = false
		system.registerOnTermination(engine.shutdown)
		engine
	}

	def buildExpression[T](filePath: Expression[String], additionalAttributes: Map[String, Any], f: String => T): Expression[T] = {

		def sspFile(filePath: String): Validation[String] = {
			val file = GatlingFiles.requestBodiesDirectory / filePath
			if (file.exists) filePath.success
			else s"Ssp body file $file doesn't exist".failure
		}

		def layout(templatePath: String, session: Session, additionalAttributes: Map[String, Any]): Validation[String] = {

			val sw = new StringWriter

			try {
				withCloseable(new PrintWriter(sw)) { pw =>
					val renderContext = new DefaultRenderContext(templatePath, sspFileEngine, pw)
					renderContext.attributes("session") = session
					for ((key, value) <- additionalAttributes) { renderContext.attributes(key) = value }

					sspFileEngine.layout(templatePath, renderContext, sessionExtraBinding)
					sw.toString.success
				}

			} catch {
				case e: Exception =>
					logger.warn("Ssp template layout failed", e)
					s"Ssp template layout failed: ${e.getMessage}".failure
			}
		}

		(session: Session) => for {
			path <- filePath(session)
			templatePath <- sspFile(path)
			body <- layout(templatePath, session, additionalAttributes)
		} yield f(body)
	}

	def asString(filePath: Expression[String], additionalAttributes: Map[String, Any]): Expression[String] = buildExpression(filePath, additionalAttributes, identity)

	def asBytes(filePath: Expression[String], additionalAttributes: Map[String, Any]): Expression[Array[Byte]] = buildExpression(filePath, additionalAttributes, _.getBytes(configuration.core.encoding))
}