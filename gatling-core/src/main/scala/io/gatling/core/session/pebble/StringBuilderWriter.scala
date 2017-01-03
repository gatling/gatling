/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.core.session.pebble

import java.io.Writer
import java.lang.{ StringBuilder => JStringBuilder }
import java.nio.charset.{ Charset, CharsetEncoder }
import java.nio.{ ByteBuffer, CharBuffer }
import java.util.concurrent.ConcurrentHashMap
import java.util.function.{ Function => JFunction }

import scala.compat.java8.FunctionConverters._

object StringBuilderWriter {
  private val concurrentMap = new ConcurrentHashMap[Charset, ThreadLocal[StringBuilderWriter]]()

  private def getThreadLocal: JFunction[Charset, ThreadLocal[StringBuilderWriter]] =
    ((charset: Charset) => new ThreadLocal[StringBuilderWriter] {
      override protected def initialValue(): StringBuilderWriter = new StringBuilderWriter(charset)
    }).asJava

  def getWriter(charset: Charset): StringBuilderWriter =
    pooled(concurrentMap.computeIfAbsent(charset, getThreadLocal))

  def pooled(threadLocal: ThreadLocal[StringBuilderWriter]): StringBuilderWriter = {
    val writer = threadLocal.get()
    writer.reset()
    writer
  }
}

class StringBuilderWriter(charset: Charset) extends Writer {

  val stringBuilder = new JStringBuilder
  val encoder: CharsetEncoder = charset.newEncoder()

  override def flush(): Unit = {}

  def reset(): Unit =
    stringBuilder.setLength(0)

  override def write(cbuf: Array[Char], off: Int, len: Int): Unit =
    throw new UnsupportedOperationException

  override def write(string: String): Unit =
    stringBuilder.append(string)

  override def write(cbuf: Array[Char]): Unit =
    stringBuilder.append(cbuf)

  override def toString: String =
    stringBuilder.toString

  def toByteBuffer(charset: Charset): ByteBuffer =
    encoder.encode(CharBuffer.wrap(stringBuilder))

  override def close(): Unit = {}
}
