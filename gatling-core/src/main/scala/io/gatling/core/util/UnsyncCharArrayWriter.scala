/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.util

import java.util.Arrays
import java.io.Writer

final class UnsyncCharArrayWriter(bufferSize: Int = 32) extends Writer {

  private var buf = new Array[Char](bufferSize)
  private var count: Int = 0

  private def ensureCapacity(newCount: Int): Unit = {
    if (newCount > buf.length) {
      buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newCount))
    }
  }

  override def write(c: Int): Unit = {
    val newCount = count + 1
    ensureCapacity(newCount)
    buf(count) = c.asInstanceOf[Char]
    count = newCount
  }

  def write(c: Array[Char], off: Int, len: Int): Unit = {
    if ((off < 0) || (off > c.length) || (len < 0) || ((off + len) > c.length) || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException
    } else if (len > 0) {
      val newCount: Int = count + len
      ensureCapacity(newCount)
      System.arraycopy(c, off, buf, count, len)
      count = newCount
    }
  }

  override def write(str: String, off: Int, len: Int): Unit = {
    val newCount: Int = count + len
    ensureCapacity(newCount)
    str.getChars(off, off + len, buf, count)
    count = newCount
  }

  def writeTo(out: Writer): Unit = out.write(buf, 0, count)

  final def printableCharSequence(csq: CharSequence) = if (csq == null) "null" else csq

  override def append(csq: CharSequence): UnsyncCharArrayWriter = {
    val s = printableCharSequence(csq).toString
    write(s, 0, s.length)
    this
  }

  override def append(csq: CharSequence, start: Int, end: Int): UnsyncCharArrayWriter = {
    val s = printableCharSequence(csq).subSequence(start, end).toString
    write(s, 0, s.length)
    this
  }

  override def append(c: Char): UnsyncCharArrayWriter = {
    write(c)
    this
  }

  def reset: Unit = count = 0

  def toCharArray: Array[Char] = Arrays.copyOf(buf, count)

  def size: Int = count

  override def toString: String = new String(buf, 0, count)

  def flush: Unit = {}

  def close: Unit = {}
}
