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
package io.gatling.core.config

import java.io.{ File => JFile, InputStream }
import java.net.URL

import scala.reflect.io.File

import org.apache.commons.io.FileUtils.copyInputStreamToFile

sealed trait Resource {
	def inputStream: InputStream
	def jfile: JFile
}

case class FileResource(file: File) extends Resource {
	def inputStream = file.inputStream
	def jfile = file.jfile
}

case class ClassPathResource(url: URL, extension: String) extends Resource {

	def inputStream = url.openStream

	def jfile = {
		val tempFile = File.makeTemp("gatling", "." + extension).jfile
		copyInputStreamToFile(inputStream, tempFile)
		tempFile
	}
}