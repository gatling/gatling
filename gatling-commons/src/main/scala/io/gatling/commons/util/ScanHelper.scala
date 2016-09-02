/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.commons.util

import java.io.{ FileInputStream, InputStream }
import java.net.JarURLConnection
import java.nio.file.{ Path, StandardCopyOption }
import java.util.jar.{ JarEntry, JarFile }

import scala.collection.JavaConversions.enumerationAsScalaIterator

import io.gatling.commons.util.Io._
import io.gatling.commons.util.PathHelper._

object ScanHelper {

  val Separator = Character.valueOf(28).toString

  sealed trait Resource {
    def path: Path
    def copyTo(target: Path): Unit
    def inputStream(): InputStream
    def lastModified: Long
  }

  case class FileResource(path: Path) extends Resource {

    private val file = path.toFile

    override def copyTo(target: Path): Unit = {
      target.getParent.mkdirs
      path.copyTo(target, StandardCopyOption.COPY_ATTRIBUTES)
    }

    override def inputStream(): InputStream = new FileInputStream(file)

    override def lastModified: Long = file.lastModified
  }

  case class JarResource(jar: JarFile, jarEntry: JarEntry) extends Resource {

    override def path = jarEntry.getName

    override def copyTo(target: Path): Unit = {
      target.getParent.mkdirs

      withCloseable(inputStream()) { input =>
        withCloseable(target.outputStream) { output =>
          input.copyTo(output)
        }
      }
    }

    override def inputStream(): InputStream = jar.getInputStream(jarEntry)

    override def lastModified: Long = jarEntry.getTime
  }

  def getPackageResources(pkg: Path, deep: Boolean): Iterator[Resource] = {

      def isResourceInRootDir(resource: Path, rootDir: Path): Boolean =
        if (resource.extension.isEmpty) false
        else if (deep) resource.startsWith(rootDir)
        else resource.getParent == rootDir

    getClass.getClassLoader.getResources(pkg.toString.replace("\\", "/")).flatMap { pkgURL =>
      pkgURL.getProtocol match {
        case "file" =>
          val rootDir: Path = pkgURL
          val files = if (deep) rootDir.deepFiles() else rootDir.files
          files.map(f => FileResource(f.path))

        case "jar" =>
          val connection = pkgURL.openConnection.asInstanceOf[JarURLConnection]
          val rootDir: Path = connection.getJarEntry.getName
          val jar = new JarFile(connection.getJarFileURL.toFile)
          jar.entries.collect {
            case jarEntry if isResourceInRootDir(jarEntry.getName, rootDir) =>
              JarResource(jar, jarEntry)
          }

        case _ => throw new UnsupportedOperationException
      }
    }
  }

  def deepCopyPackageContent(pkg: Path, targetDirectoryPath: Path): Unit = {

      def getPathStringAfterPackage(path: Path, pkg: Path): Path = {
        val pathString = path.segments.mkString(Separator)
        val pkgString = pkg.segments.mkString(Separator)
        segments2path(pathString.split(pkgString).last.split(Separator))
      }

    getPackageResources(pkg, deep = true).foreach { resource =>
      val target = targetDirectoryPath / getPathStringAfterPackage(resource.path, pkg)
      resource.copyTo(target)
    }
  }
}
