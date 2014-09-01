package io.gatling.core.util

import java.io.{ File, FileOutputStream, OutputStreamWriter }
import java.net.URI

import scala.reflect.io.Path

import io.gatling.core.config.GatlingConfiguration.configuration

object UriHelper {

  def pathToUri(s: String) = new File(s).toURI

  implicit class RichUri(val uri: URI) extends AnyVal {

    def /(child: String): URI =
      if (new File(uri.resolve(child)).isDirectory) uri.resolve(s"$child/") else uri.resolve(s"$child")

    def /(child: URI) = uri.resolve(child)

    def parent = uri.resolve(".")

    def toFile = new File(uri)

    def toPath = Path(toFile)

    def ifFile[T](f: File => T): Option[T] = {
      val file = uri.toFile
      if (file.isFile) Some(f(file)) else None
    }

    def inputStream() = uri.toURL.openStream()

    def writer(append: Boolean = false) =
      new OutputStreamWriter(new FileOutputStream(uri.toFile, append), configuration.core.codec.name)
  }

}
