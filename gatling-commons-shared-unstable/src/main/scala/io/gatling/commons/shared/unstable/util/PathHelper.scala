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

import java.io._
import java.nio.charset.Charset
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

final case class CachingPath(path: Path) {

  override def toString: String = path.toString

  lazy val filename: String = path.getFileName.toString
}

object PathHelper {

  implicit class RichPath(val path: Path) extends AnyVal {

    def /(pathString: String): Path = path.resolve(pathString)

    def /(other: Path): Path = path.resolve(other)

    def filename: String = path.getFileName.toString

    def exists: Boolean = Files.exists(path)

    def mkdirs(): Path = Files.createDirectories(path)

    def createFile(): Path = Files.createFile(path)

    def delete(): Unit = Files.delete(path)

    def inputStream: InputStream = Files.newInputStream(path)

    def outputStream: OutputStream = Files.newOutputStream(path)

    def isFile: Boolean = Files.isRegularFile(path)

    def isDirectory: Boolean = Files.isDirectory(path)

    def segments: List[Path] = path.iterator.asScala.toList

    def ancestor(n: Int): Path = {
      @tailrec
      def loop(path: Path, n: Int): Path =
        n match {
          case 0 => path
          case _ => loop(path.getParent, n - 1)
        }

      require(n >= 0, s"ancestor rank must be positive but asked for $n")
      val length = path.segments.length
      require(n <= length, s"can't ask for ancestor rank $n while segments length is $length")
      loop(path, n)
    }

    def ifFile[T](f: File => T): Option[T] = if (isFile) Some(f(path.toFile)) else None

    def writer(charset: Charset): Writer = Files.newBufferedWriter(path, charset)

    def copyTo(other: Path, options: CopyOption*): Path = Files.copy(path, other, options: _*)

    def extension: String = {
      val pathString = path.toString
      val dotIndex = pathString.lastIndexOf('.')
      if (dotIndex == -1) "" else pathString.substring(dotIndex + 1)
    }

    def hasExtension(ext: String): Boolean =
      extension.equalsIgnoreCase(ext)

    def stripExtension: String = filename.stripSuffix("." + extension)

    def deepFiles(f: CachingPath => Boolean): Seq[CachingPath] = deepFiles(f, Int.MaxValue)
    def deepFiles(f: CachingPath => Boolean, maxDepth: Int): Seq[CachingPath] =
      if (path.exists) {
        val acc = new collection.mutable.ArrayBuffer[CachingPath]
        Files.walkFileTree(
          path,
          new SimpleFileVisitor[Path] {
            override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
              val cachingPath = CachingPath(file)
              if (f(cachingPath))
                acc += cachingPath
              super.visitFile(path, attrs)
            }
          }
        )
        acc.toSeq
      } else {
        Nil
      }

    def files: Seq[CachingPath] = deepFiles(_ => true, maxDepth = 1)

    def deepDirs(f: CachingPath => Boolean): Seq[CachingPath] =
      if (path.exists) {
        val acc = new collection.mutable.ArrayBuffer[CachingPath]
        Files.walkFileTree(
          path,
          new SimpleFileVisitor[Path] {
            override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
              val cachingPath = CachingPath(dir)
              if (f(cachingPath))
                acc += cachingPath
              super.preVisitDirectory(path, attrs)
            }
          }
        )
        acc.toSeq
      } else {
        Nil
      }
  }
}
