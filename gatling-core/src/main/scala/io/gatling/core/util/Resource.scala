/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.util

import java.io.{ File, FileInputStream, FileOutputStream, InputStream }
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

import io.gatling.commons.util.Io._
import io.gatling.commons.util.PathHelper._
import io.gatling.commons.validation._
import io.gatling.core.config.{ GatlingConfiguration, GatlingFiles }

object Resource {

  private case class Location(directory: Path, path: String)

  private object ClasspathResource {
    def unapply(location: Location): Option[Validation[Resource]] =
      Option(getClass.getClassLoader.getResource(location.path.replace('\\', '/'))).map { url =>
        url.getProtocol match {
          case "file" => Resource(url.jfile).success
          case "jar"  => Resource(url).success
          case _      => s"$url is neither a file nor a jar".failure
        }
      }
  }

  private object DirectoryChildResource {
    def unapply(location: Location): Option[Validation[Resource]] =
      (location.directory / location.path).ifFile { f =>
        if (f.canRead)
          Resource(f).success
        else
          s"File $f can't be read".failure
      }
  }

  private object AbsoluteFileResource {
    def unapply(location: Location): Option[Validation[Resource]] =
      string2path(location.path).ifFile(Resource(_).success)
  }

  private[gatling] def resolveResource(directory: Path, path: String): Validation[Resource] =
    Location(directory, path) match {
      case ClasspathResource(res)      => res
      case DirectoryChildResource(res) => res
      case AbsoluteFileResource(res)   => res
      case _                           => s"Resource $path not found".failure
    }

  def resolveResource(path: String)(implicit configuration: GatlingConfiguration): Validation[Resource] =
    resolveResource(GatlingFiles.resourcesDirectory, path)

  def apply(file: File): Resource = Resource(file.getName, file)
  def apply(url: URL): Resource = {
    val name = {
      val urlString = url.toString
      urlString.lastIndexOf(File.separatorChar) match {
        case -1 => urlString
        case i  => urlString.substring(i + 1)
      }
    }

    val file = {
      val tempFile = File.createTempFile("gatling-" + name, null)
      tempFile.deleteOnExit()

      withCloseable(url.openStream()) { is =>
        withCloseable(new FileOutputStream(tempFile, false)) { os =>
          is.copyTo(os)
        }
      }
      tempFile
    }

    Resource(name, file)
  }
}

case class Resource(name: String, file: File) {
  def inputStream: InputStream = new FileInputStream(file)
  def string(charset: Charset): String = withCloseable(inputStream) { _.toString(charset) }
  def bytes: Array[Byte] = file.toByteArray
}

trait ResourceCache {
  private val resourceCache = new ConcurrentHashMap[String, Validation[Resource]]()

  protected def cachedResource(path: String)(implicit configuration: GatlingConfiguration): Validation[Resource] =
    resourceCache.computeIfAbsent(path, Resource.resolveResource)
}
