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
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// this is a for from commons-io
package io.gatling.commons.util

import java.io.{ InputStream, OutputStream }

object FastByteArrayOutputStream {

  private[this] val Pool = new ThreadLocal[FastByteArrayOutputStream] {
    override protected def initialValue(): FastByteArrayOutputStream = new FastByteArrayOutputStream
  }

  def pooled(): FastByteArrayOutputStream = {
    val os = Pool.get()
    os.reset()
    os
  }
}

class FastByteArrayOutputStream(initialSize: Int = 1024) extends OutputStream {

  private val buffers = collection.mutable.ArrayBuffer.empty[Array[Byte]]
  private var currentBufferIndex = 0
  private var filledBufferSum = 0
  private var currentBuffer: Array[Byte] = _
  private var count = 0
  private var reuseBuffers = true

  needNewBuffer(initialSize)

  private def needNewBuffer(newcount: Int): Unit = {
    if (currentBufferIndex < buffers.size - 1) {
      //Recycling old buffer
      filledBufferSum += currentBuffer.length

      currentBufferIndex += 1
      currentBuffer = buffers(currentBufferIndex)
    } else {
      //Creating new buffer
      var newBufferSize = 0
      if (currentBuffer == null) {
        newBufferSize = newcount
        filledBufferSum = 0
      } else {
        newBufferSize = Math.max(currentBuffer.length << 1, newcount - filledBufferSum)
        filledBufferSum += currentBuffer.length
      }

      currentBufferIndex += 1
      currentBuffer = new Array[Byte](newBufferSize)
      buffers += currentBuffer
    }
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    if ((off < 0)
      || (off > b.length)
      || (len < 0)
      || ((off + len) > b.length)
      || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException()

    } else if (len != 0) {
      val newCount = count + len
      var remaining = len
      var inBufferPos = count - filledBufferSum
      while (remaining > 0) {
        val part = Math.min(remaining, currentBuffer.length - inBufferPos)
        System.arraycopy(b, off + len - remaining, currentBuffer, inBufferPos, part)
        remaining -= part
        if (remaining > 0) {
          needNewBuffer(newCount)
          inBufferPos = 0
        }
      }
      count = newCount
    }
  }

  def write(b: Int): Unit = {
    var inBufferPos = count - filledBufferSum
    if (inBufferPos == currentBuffer.length) {
      needNewBuffer(count + 1)
      inBufferPos = 0
    }
    currentBuffer(inBufferPos) = b.toByte
    count += 1
  }

  def write(in: InputStream): Int = {
    var readCount = 0
    var inBufferPos = count - filledBufferSum
    var n = in.read(currentBuffer, inBufferPos, currentBuffer.length - inBufferPos)
    while (n != -1) {
      readCount += n
      inBufferPos += n
      count += n
      if (inBufferPos == currentBuffer.length) {
        needNewBuffer(currentBuffer.length)
        inBufferPos = 0
      }
      n = in.read(currentBuffer, inBufferPos, currentBuffer.length - inBufferPos)
    }
    readCount
  }

  def size: Int = count

  override def close(): Unit = {}

  def reset(): Unit = {
    count = 0
    filledBufferSum = 0
    currentBufferIndex = 0
    if (reuseBuffers) {
      currentBuffer = buffers(currentBufferIndex)
    } else {
      //Throw away old buffers
      currentBuffer = null
      val size = buffers(0).length
      buffers.clear()
      needNewBuffer(size)
      reuseBuffers = true
    }
  }

  def writeTo(out: OutputStream): Unit = {
    var remaining = count
    buffers.foreach { buf =>
      val c = Math.min(buf.length, remaining)
      out.write(buf, 0, c)
      remaining -= c
      if (remaining == 0) {
        return
      }
    }
  }

  def toByteArray: Array[Byte] = {
    var remaining = count
    if (remaining == 0) {
      Bytes.EmptyBytes
    } else {
      val newbuf = new Array[Byte](remaining)
      var pos = 0
      buffers.foreach { buf =>
        val c = Math.min(buf.length, remaining)
        System.arraycopy(buf, 0, newbuf, pos, c)
        pos += c
        remaining -= c
        if (remaining == 0) {
          return newbuf
        }
      }

      newbuf
    }
  }
}
