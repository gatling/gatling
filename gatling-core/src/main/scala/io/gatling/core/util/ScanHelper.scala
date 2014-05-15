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

import java.io.{ File => JFile }
import java.net.JarURLConnection

import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.tools.nsc.io.{ File, Fileish, Jar, Path }

import org.apache.commons.io.IOUtils

import io.gatling.core.util.IO._

object ScanHelper {

  val SEPARATOR = Character.valueOf(28).toString

  def getPackageResources(pkg: Path, deep: Boolean): Iterator[Resource] = {

      def isResourceInRootDir(fileish: Fileish, rootDir: Path): Boolean = {
        if (fileish.path.extension.isEmpty)
          false
        else if (deep)
          fileish.path.startsWith(rootDir)
        else
          fileish.parent == rootDir
      }

    getClass.getClassLoader.getResources(pkg.toString.replace("\\", "/")).flatMap { pkgURL =>
      pkgURL.getProtocol match {
        case "file" =>
          val rootDir = File(pkgURL.jfile).toDirectory
          val files = if (deep) rootDir.deepFiles else rootDir.files
          files.map(FileResource)

        case "jar" =>
          val connection = pkgURL.openConnection.asInstanceOf[JarURLConnection]
          val rootDir: Path = connection.getJarEntry.getName
          val jar = new Jar(File(new JFile(connection.getJarFileURL.toURI)))
          jar.fileishIterator.collect { case fileish if isResourceInRootDir(fileish, rootDir) => new FileishResource(fileish) }

        case _ => throw new UnsupportedOperationException
      }
    }
  }

  def deepCopyPackageContent(pkg: Path, targetDirectoryPath: Path) {

      def getPathStringAfterPackage(path: Path, pkg: Path): Path = {
        val pathString = path.segments.mkString(SEPARATOR)
        val pkgString = pkg.segments.mkString(SEPARATOR)
        Path(pathString.split(pkgString).last.split(SEPARATOR))
      }

    getPackageResources(pkg, deep = true).foreach { resource =>
      val target = targetDirectoryPath / getPathStringAfterPackage(resource.path, pkg)
      resource.copyTo(target)
    }
  }
}

sealed trait Resource {
  def path: Path
  def copyTo(target: Path)
}

case class FileResource(file: File) extends Resource {
  def path = file.path
  def copyTo(target: Path) {
    target.parent.createDirectory()
    file.copyTo(target, preserveFileDate = true)
  }
}

case class FileishResource(fileish: Fileish) extends Resource with IO {
  def path = fileish.path
  def copyTo(target: Path) {
    target.parent.createDirectory()

    withCloseable(fileish.input()) { input =>
      withCloseable(target.toFile.outputStream(append = false)) { output =>
        IOUtils.copy(input, output)
      }
    }
  }
}
