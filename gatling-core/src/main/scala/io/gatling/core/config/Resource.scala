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

import java.io.{ File => JFile, FileOutputStream, InputStream }
import java.net.URL

import scala.reflect.io.{ File, Path }
import scala.tools.nsc.io.Path.string2path

import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.core.util.IO._

object Resource {

  object ClasspathResource {
    def unapply(location: Location): Option[Validation[Resource]] =
      Option(getClass.getClassLoader.getResource(location.path.toString().replace('\\', '/'))).map { url =>
        url.getProtocol match {
          case "file" => FileResource(File(url.jfile)).success
          case "jar"  => ArchiveResource(url, location.path.extension).success
          case _      => s"$url is neither a file nor a jar".failure
        }
      }
  }

  object FileInFolderResource {
    def unapply(location: Location): Option[Validation[Resource]] =
      (location.directory / location.path).ifFile(f => FileResource(f.toFile).success)
  }

  object AbsoluteFileResource {
    def unapply(location: Location): Option[Validation[Resource]] =
      location.path.ifFile(f => FileResource(f.toFile).success)
  }

  private def load(directory: Path, path: Path): Validation[Resource] =
    new Location(directory, path) match {
      case ClasspathResource(res)    => res
      case FileInFolderResource(res) => res
      case AbsoluteFileResource(res) => res
      case _                         => s"file $path doesn't exist".failure
    }

  private class Location(val directory: Path, val path: Path)

  def feeder(fileName: String): Validation[Resource] = load(GatlingFiles.dataDirectory, fileName)
  def requestBody(fileName: String): Validation[Resource] = load(GatlingFiles.requestBodiesDirectory, fileName)
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

    withCloseable(inputStream) { is =>
      withCloseable(new FileOutputStream(tempFile, false)) { os =>
        is.copyTo(os)
      }
    }
    tempFile
  }
}
