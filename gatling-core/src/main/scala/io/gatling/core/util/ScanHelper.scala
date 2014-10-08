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
package io.gatling.core.util

import java.io.InputStream
import java.net.JarURLConnection
import java.nio.file.{ Path, StandardCopyOption }
import java.util.jar.JarFile

import scala.collection.JavaConversions.enumerationAsScalaIterator

import io.gatling.core.util.IO._
import io.gatling.core.util.PathHelper._

object ScanHelper {

  val SEPARATOR = Character.valueOf(28).toString

  def getPackageResources(pkg: Path, deep: Boolean): Iterator[Resource] = {

      def isResourceInRootDir(resource: Path, rootDir: Path): Boolean =
        if (resource.extension.isEmpty) false
        else if (deep) resource.startsWith(rootDir)
        else resource.getParent == rootDir

    getClass.getClassLoader.getResources(pkg.toString.replace("\\", "/")).flatMap { pkgURL =>
      pkgURL.getProtocol match {
        case "file" =>
          val rootDir: Path = pkgURL
          val files = if (deep) rootDir.deepFiles else rootDir.files
          files.map(FileResource)

        case "jar" =>
          val connection = pkgURL.openConnection.asInstanceOf[JarURLConnection]
          val rootDir: Path = connection.getJarEntry.getName
          val jar = new JarFile(connection.getJarFileURL.toFile)
          jar.entries.collect {
            case jarEntry if isResourceInRootDir(jarEntry.getName, rootDir) =>
              JarResource(jarEntry.getName, jar.getInputStream(jarEntry))
          }

        case _ => throw new UnsupportedOperationException
      }
    }
  }

  def deepCopyPackageContent(pkg: Path, targetDirectoryPath: Path) {

      def getPathStringAfterPackage(path: Path, pkg: Path): Path = {
        val pathString = path.segments.mkString(SEPARATOR)
        val pkgString = pkg.segments.mkString(SEPARATOR)
        segments2path(pathString.split(pkgString).last.split(SEPARATOR))
      }

    getPackageResources(pkg, deep = true).foreach { resource =>
      val target = targetDirectoryPath / getPathStringAfterPackage(resource.path, pkg)
      resource.copyTo(target)
    }
  }
}

sealed trait Resource {
  def path: Path
  def copyTo(target: Path): Unit
}

case class FileResource(path: Path) extends Resource {
  def copyTo(target: Path) {
    target.getParent.mkdirs
    path.copyTo(target, StandardCopyOption.COPY_ATTRIBUTES)
  }
}

case class JarResource(path: Path, inputStream: InputStream) extends Resource {
  def copyTo(target: Path) {
    target.getParent.mkdirs

    withCloseable(inputStream) { input =>
      withCloseable(target.outputStream) { output =>
        input.copyTo(output)
      }
    }
  }
}
