/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.commons.shared.unstable.util

import java.io.{ BufferedInputStream, FileInputStream, InputStream }
import java.net.JarURLConnection
import java.nio.file.{ Path, Paths, StandardCopyOption }
import java.util.jar.{ JarEntry, JarFile }

import scala.jdk.CollectionConverters._
import scala.util.Using

import io.gatling.commons.shared.unstable.util.PathHelper._
import io.gatling.commons.util.Io._

object ScanHelper {

  private val Separator = Character.valueOf(28).toString

  sealed trait Resource {
    def path: Path
    def copyTo(target: Path): Unit
    def inputStream(): InputStream
    def lastModified: Long
  }

  private final case class FileResource(path: Path) extends Resource {

    private val file = path.toFile

    override def copyTo(target: Path): Unit = {
      target.getParent.mkdirs()
      path.copyTo(target, StandardCopyOption.COPY_ATTRIBUTES)
    }

    override def inputStream(): InputStream = new BufferedInputStream(new FileInputStream(file))

    override def lastModified: Long = file.lastModified
  }

  private final case class JarResource(jar: JarFile, jarEntry: JarEntry) extends Resource {

    override def path: Path = Paths.get(jarEntry.getName)

    override def copyTo(target: Path): Unit = {
      target.getParent.mkdirs()
      Using.resources(jar.getInputStream(jarEntry), target.outputStream) { (in, out) =>
        in.copyTo(out)
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

    getClass.getClassLoader.getResources(pkg.toString.replace("\\", "/")).asScala.flatMap { pkgURL =>
      pkgURL.getProtocol match {
        case "file" =>
          val rootDir = Paths.get(pkgURL.toURI)
          val files = if (deep) rootDir.deepFiles(_ => true) else rootDir.files
          files.map(f => FileResource(f.path))

        case "jar" =>
          val connection = pkgURL.openConnection.asInstanceOf[JarURLConnection]
          val rootDir = Paths.get(connection.getJarEntry.getName)
          val jar = new JarFile(Paths.get(connection.getJarFileURL.toURI).toFile)
          jar.entries.asScala.collect {
            case jarEntry if isResourceInRootDir(Paths.get(jarEntry.getName), rootDir) =>
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
      val segments = pathString.split(pkgString).last.split(Separator)
      Paths.get(segments.head, segments.tail: _*)
    }

    getPackageResources(pkg, deep = true).foreach { resource =>
      val target = targetDirectoryPath / getPathStringAfterPackage(resource.path, pkg)
      resource.copyTo(target)
    }
  }
}
