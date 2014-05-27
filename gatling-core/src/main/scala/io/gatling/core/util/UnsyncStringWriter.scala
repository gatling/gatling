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

import java.lang.{ StringBuilder => JStringBuilder }
import java.io.Writer

final class UnsyncStringWriter(initialSize: Int = 16) extends Writer {

  private val buf: JStringBuilder = new JStringBuilder(initialSize)

  override def write(c: Int): Unit = buf.append(c.asInstanceOf[Char])

  def write(cbuf: Array[Char], off: Int, len: Int): Unit =
    if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException

    } else if (len > 0) {
      buf.append(cbuf, off, len)
    }

  override def write(str: String): Unit = buf.append(str)

  override def write(str: String, off: Int, len: Int): Unit = buf.append(str.substring(off, off + len))

  override def append(csq: CharSequence): UnsyncStringWriter = {
    if (csq == null) write("null")
    else write(csq.toString)
    this
  }

  override def append(csq: CharSequence, start: Int, end: Int): UnsyncStringWriter = {
    val cs: CharSequence = if (csq == null) "null" else csq
    write(cs.subSequence(start, end).toString)
    this
  }

  override def append(c: Char): UnsyncStringWriter = {
    write(c)
    this
  }

  override def toString: String = buf.toString

  def getBuffer: JStringBuilder = buf

  def flush: Unit = {}

  def close: Unit = {}
}
