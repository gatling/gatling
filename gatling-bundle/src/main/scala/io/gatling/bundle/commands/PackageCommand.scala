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

package io.gatling.bundle.commands

import java.io.{ File, FileOutputStream }
import java.nio.file.{ FileVisitResult, Files, Path, Paths, SimpleFileVisitor }
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.{ Attributes, JarEntry, JarOutputStream, Manifest }
import java.util.zip.ZipOutputStream

import scala.util.Using

import io.gatling.bundle.CommandArguments
import io.gatling.bundle.commands.CommandHelper._

class PackageCommand(args: List[String]) {

  private[bundle] def run(): File = {
    compile(args)
    println("Creating the package")
    createJar()
  }

  private def createJar(): File = {
    val file = File.createTempFile("package", ".jar")
    file.deleteOnExit()

    Using(new JarOutputStream(new FileOutputStream(file))) { jos =>
      jos.setLevel(ZipOutputStream.STORED)
      val je = new JarEntry("META-INF/MANIFEST.MF")
      jos.putNextEntry(je)
      val manifest = new Manifest
      manifest.getMainAttributes.putValue(Attributes.Name.MANIFEST_VERSION.toString, "1.0")
      manifest.getMainAttributes.putValue(Attributes.Name.SIGNATURE_VERSION.toString, "GatlingCorp")
      manifest.getMainAttributes.putValue("Gatling-Packager", "bundle")
      manifest.getMainAttributes.putValue("Gatling-Version", gatlingVersion)
      manifest.write(jos)

      val pathTestClasses = Paths.get(s"$gatlingHome${File.separator}target${File.separator}test-classes").toAbsolutePath
      addJarEntries(pathTestClasses, jos)

      val pathResources = Paths.get(s"$gatlingHome${File.separator}user-files${File.separator}resources").toAbsolutePath
      addJarEntries(pathResources, jos)
    }
    println("Package created")
    file
  }

  private def addJarEntries(rootPath: Path, jos: JarOutputStream) = {
    Files.walkFileTree(
      rootPath,
      new SimpleFileVisitor[Path] {
        override def visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult = {
          jos.putNextEntry(new JarEntry(rootPath.relativize(path).toString))
          if (Files.isRegularFile(path)) {
            Using(Files.newInputStream(path)) { reader =>
              var fileOut = -1
              fileOut = reader.read()
              while (fileOut != -1) {
                jos.write(fileOut)
                fileOut = reader.read()
              }
            }
            jos.flush()
          }
          jos.closeEntry()

          super.visitFile(rootPath, attrs)
        }
      }
    )
  }
}
