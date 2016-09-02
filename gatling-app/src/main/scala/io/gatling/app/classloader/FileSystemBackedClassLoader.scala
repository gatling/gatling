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
package io.gatling.app.classloader

import java.net.{ URL, URLConnection, URLStreamHandler }
import java.nio.file.Path
import java.security.cert.Certificate
import java.security.{ CodeSource, ProtectionDomain }

import scala.collection.mutable

import io.gatling.commons.util.Io._
import io.gatling.commons.util.PathHelper._

private[classloader] class FileSystemBackedClassLoader(root: Path, parent: ClassLoader)
    extends ClassLoader(parent) {

  def classNameToPath(name: String): Path =
    if (name endsWith ".class") name
    else name.replace('.', '/') + ".class"

  def dirNameToPath(name: String): Path =
    name.replace('.', '/')

  def findPath(path: Path): Option[Path] = {
    val fullPath = root / path
    if (fullPath.exists) Some(fullPath) else None
  }

  override def findResource(name: String) = findPath(name).map { path =>
    new URL(null, "repldir:" + path, new URLStreamHandler {
      override def openConnection(url: URL): URLConnection = new URLConnection(url) {
        override def connect(): Unit = ()
        override def getInputStream = path.inputStream
      }
    })
  }.orNull

  override def getResourceAsStream(name: String) = findPath(name) match {
    case Some(path) => path.inputStream
    case None       => super.getResourceAsStream(name)
  }

  def classAsStream(className: String) =
    Option(getResourceAsStream(className.replaceAll("""\.""", "/") + ".class"))

  def classBytes(name: String): Array[Byte] = findPath(classNameToPath(name)) match {
    case Some(path) => path.inputStream.toByteArray()
    case None => classAsStream(name) match {
      case Some(stream) => stream.toByteArray()
      case None         => Array.empty
    }
  }

  override def findClass(name: String): Class[_] = {
    val bytes = classBytes(name)
    if (bytes.length == 0) throw new ClassNotFoundException(name)
    else defineClass(name, bytes, 0, bytes.length, protectionDomain)
  }

  private val packages = mutable.Map[String, Package]()

  lazy val protectionDomain = {
    val cl = Thread.currentThread.getContextClassLoader
    val resource = cl.getResource("scala/runtime/package.class")
    if (resource == null || resource.getProtocol != "jar") null else {
      val s = resource.getPath
      val n = s.lastIndexOf('!')
      if (n < 0) null else {
        val path = s.substring(0, n)
        new ProtectionDomain(new CodeSource(new URL(path), null.asInstanceOf[Array[Certificate]]), null, this, null)
      }
    }
  }

  override def definePackage(name: String, specTitle: String,
                             specVersion: String, specVendor: String,
                             implTitle: String, implVersion: String,
                             implVendor: String, sealBase: URL) = {
    throw new UnsupportedOperationException()
  }

  override def getPackage(name: String) = findPath(dirNameToPath(name)) match {
    case Some(path) => packages.getOrElseUpdate(name, {
      val ctor = classOf[Package].getDeclaredConstructor(
        classOf[String], classOf[String], classOf[String],
        classOf[String], classOf[String], classOf[String],
        classOf[String], classOf[URL], classOf[ClassLoader]
      )
      ctor.setAccessible(true)
      ctor.newInstance(name, null, null, null, null, null, null, null, this)
    })
    case None => super.getPackage(name)
  }

  override def getPackages =
    root.deepDirs().map(path => getPackage(path.toString)).toArray

}
