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

import java.io.{ BufferedInputStream, ByteArrayInputStream }

import org.apache.commons.io.FileUtils

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles
import io.gatling.core.session.{ EL, Expression, Session }
import io.gatling.core.validation.Validation

object ELTemplateBodies {

	val elTemplateBodiesCache = new collection.mutable.HashMap[String, Validation[Expression[String]]]

	def compileTemplate(path: String): Validation[Expression[String]] =
		GatlingFiles.requestBodyFile(path)
			.map(f => FileUtils.readFileToString(f.jfile, configuration.simulation.encoding))
			.map(EL.compile[String])

	def buildExpression[T](filePath: Expression[String], f: String => T): Expression[T] = (session: Session) =>
		for {
			path <- filePath(session)
			expression <- elTemplateBodiesCache.getOrElseUpdate(path, compileTemplate(path))
			body <- expression(session)
		} yield f(body)

	def asString(filePath: Expression[String]) = {
		val string = buildExpression(filePath, identity)
		new StringBody(string)
	}

	def asBytes(filePath: Expression[String]): ByteArrayBody = {
		val bytes = buildExpression(filePath, _.getBytes(configuration.simulation.encoding))
		new ByteArrayBody(bytes)
	}

	def asStream(filePath: Expression[String]): InputStreamBody = {
		val stream = buildExpression(filePath, string => new BufferedInputStream(new ByteArrayInputStream(string.getBytes(configuration.simulation.encoding))))
		new InputStreamBody(stream)
	}
}