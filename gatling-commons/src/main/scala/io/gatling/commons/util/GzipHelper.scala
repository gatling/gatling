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

import java.io.{ InputStream, OutputStream }
import java.util.zip.{ CRC32, Deflater, DeflaterOutputStream }

import io.gatling.commons.util.Io._

object GzipHelper {

  def gzip(string: String): Array[Byte] = gzip(string.getBytes)

  def gzip(bytes: Array[Byte]): Array[Byte] =
    gzip(new FastByteArrayInputStream(bytes))

  def gzip(in: InputStream): Array[Byte] =
    withCloseable(in) { is =>
      val out = FastByteArrayOutputStream.pooled()
      val gzip = ReusableGzipOutputStream.forStream(out)
      try {
        gzip.writeHeader()
        in.copyTo(gzip, 1024)
        gzip.finish()
      } finally {
        gzip.reset()
      }

      out.toByteArray
    }
}

class SettableOutputStream(var target: OutputStream) extends OutputStream {

  override def write(b: Array[Byte], off: Int, len: Int): Unit =
    target.write(b, off, len)

  override def write(b: Array[Byte]): Unit =
    target.write(b)

  override def write(b: Int): Unit =
    target.write(b.toByte)
}

object ReusableGzipOutputStream {

  private val GzipMagic = 0x8b1f

  private val TrailerSize = 8

  private val Streams = new ThreadLocal[ReusableGzipOutputStream] {
    override protected def initialValue() = new ReusableGzipOutputStream(new SettableOutputStream(null))
  }

  def forStream(target: OutputStream): ReusableGzipOutputStream = {
    val gzip = Streams.get()
    gzip.os.target = target
    gzip
  }
}

class ReusableGzipOutputStream(val os: SettableOutputStream)
    extends DeflaterOutputStream(os, new Deflater(Deflater.DEFAULT_COMPRESSION, true)) {

  import ReusableGzipOutputStream._

  private val crc = new CRC32

  override def write(buf: Array[Byte], off: Int, len: Int): Unit = {
    super.write(buf, off, len)
    crc.update(buf, off, len)
  }

  override def finish(): Unit =
    if (!`def`.finished) {
      `def`.finish()
      while (!`def`.finished) {
        var len = `def`.deflate(buf, 0, buf.length)
        if (`def`.finished && len <= buf.length - TrailerSize) {
          writeTrailer(buf, len)
          len = len + TrailerSize
          os.write(buf, 0, len)
          return
        }
        if (len > 0)
          out.write(buf, 0, len)
      }
      val trailer = Array.ofDim[Byte](TrailerSize)
      writeTrailer(trailer, 0)
      out.write(trailer)
    }

  def writeHeader(): Unit =
    os.write(Array(
      GzipMagic.toByte, // Magic number (short)
      (GzipMagic >> 8).toByte, // Magic number (short)
      Deflater.DEFLATED.toByte, // Compression method (CM)
      0.toByte, // Flags (FLG)
      0.toByte, // Modification time MTIME (int)
      0.toByte, // Modification time MTIME (int)
      0.toByte, // Modification time MTIME (int)
      0.toByte, // Modification time MTIME (int)
      0.toByte, // Extra flags (XFLG)
      0.toByte // Operating system (OS)
    ))

  private def writeTrailer(buf: Array[Byte], offset: Int): Unit = {
    writeInt(crc.getValue.toInt, buf, offset)
    writeInt(`def`.getTotalIn, buf, offset + 4)
  }

  /*
   * Writes integer in Intel byte order to a byte array, starting at a given offset.
   */
  private def writeInt(i: Int, buf: Array[Byte], offset: Int): Unit = {
    writeShort(i & 0xffff, buf, offset)
    writeShort((i >> 16) & 0xffff, buf, offset + 2)
  }

  /*
   * Writes short integer in Intel byte order to a byte array, starting at a given offset
   */
  private def writeShort(s: Int, buf: Array[Byte], offset: Int): Unit = {
    buf(offset) = (s & 0xff).toByte
    buf(offset + 1) = ((s >> 8) & 0xff).toByte
  }

  def reset(): Unit = {
    crc.reset()
    `def`.reset()
    os.target = null
  }
}
