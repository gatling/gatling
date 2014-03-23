/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.reflect.io.{ File, Path }
import scala.tools.nsc.io.Path.string2path

import org.apache.commons.io.FileUtils.copyInputStreamToFile

import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.core.util.FileHelper.RichURL

object Resource {

	private def load(filePath: Path, fileSystemFolder: Path): Validation[Resource] = {
		val classPathResource = Option(getClass.getClassLoader.getResource(filePath.toString().replace('\\', '/'))).map { url =>
			url.getProtocol match {
				case "file" => FileResource(File(url.jfile()))
				case "jar" => ArchiveResource(url, filePath.extension)
				case _ => throw new UnsupportedOperationException
			}
		}

		classPathResource.orElse((fileSystemFolder / filePath).ifFile(path => FileResource(path.toFile))) match {
			case Some(resource) => resource.success
			case _ => s"file $filePath doesn't exist".failure
		}
	}

	def feeder(fileName: String): Validation[Resource] = load(fileName, GatlingFiles.dataDirectory)
	def requestBody(fileName: String): Validation[Resource] = load(fileName, GatlingFiles.requestBodiesDirectory)
}

sealed trait Resource {
	def inputStream: InputStream
	def jfile: JFile
}

case class FileResource(file: File) extends Resource {
	def inputStream = file.inputStream()
	def jfile = file.jfile
}

case class ArchiveResource(url: URL, extension: String) extends Resource {

	def inputStream = url.openStream

	def jfile = {
		val tempFile = File.makeTemp("gatling", "." + extension).jfile
		copyInputStreamToFile(inputStream, tempFile)
		tempFile
	}
}
