/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import scala.jdk.CollectionConverters._

object PathHelper {

  // can't extend Path, even if interface is public
  // Files.newInputStream would crash with a java.nio.file.ProviderMismatchException
  final case class CachingPath(path: Path) {

    override def toString: String = path.toString

    lazy val filename: String = path.getFileName.toString
  }

  def deepDirs(path: Path): Seq[CachingPath] = deepDirs(path, _ => true)

  def deepDirs(path: Path, f: CachingPath => Boolean): Seq[CachingPath] =
    if (Files.exists(path)) {
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

  def deepFiles(path: Path): Seq[CachingPath] = deepFiles(path, _ => true)

  def deepFiles(path: Path, f: CachingPath => Boolean): Seq[CachingPath] =
    if (Files.exists(path)) {
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

  def files(path: Path): Seq[CachingPath] =
    Files
      .list(path)
      .iterator
      .asScala
      .filter(file => !Files.isDirectory(file))
      .map(CachingPath(_))
      .toList

  implicit class RichPath(val path: Path) extends AnyVal {

    def extension: String = {
      val pathString = path.toString
      val dotIndex = pathString.lastIndexOf('.')
      if (dotIndex == -1) "" else pathString.substring(dotIndex + 1)
    }

    def hasExtension(ext: String): Boolean =
      extension.equalsIgnoreCase(ext)

    def stripExtension: String = path.getFileName.toString.stripSuffix("." + extension)
  }
}
