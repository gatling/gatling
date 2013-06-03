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

import java.io.File

import scala.collection.mutable

import org.apache.commons.io.FileUtils

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.Validation

object RawFileBodies {

	private val rawFileBodiesCache = mutable.Map.empty[String, Validation[File]]

	def buildExpression[T](filePath: Expression[String], f: File => T): Expression[T] = (session: Session) =>
		for {
			path <- filePath(session)
			file <- rawFileBodiesCache.getOrElseUpdate(path, GatlingFiles.requestBodyResource(path).map(_.jfile))
		} yield f(file)

	def asFile(filePath: Expression[String]): Expression[File] = buildExpression(filePath, identity)

	def asString(filePath: Expression[String]): Expression[String] = buildExpression(filePath, FileUtils.readFileToString(_, configuration.core.encoding))

	def asBytes(filePath: Expression[String]): Expression[Array[Byte]] = buildExpression(filePath, FileUtils.readFileToByteArray)
}