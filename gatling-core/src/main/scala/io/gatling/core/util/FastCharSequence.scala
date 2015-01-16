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

import StringHelper._

object FastCharSequence {

  val Empty = new FastCharSequence(Array.empty, 0, 0)

  def apply(s: String): FastCharSequence = {
    val chars = s.unsafeChars
    new FastCharSequence(chars, 0, chars.length)
  }
}

case class FastCharSequence(charArray: Array[Char], offset: Int, length: Int) extends CharSequence {

  def charAt(index: Int): Char = charArray(offset + index)

  def subSequence(start: Int): FastCharSequence = subSequence(start, length)

  def subSequence(start: Int, end: Int): FastCharSequence = new FastCharSequence(charArray, offset + start, end - start)

  def startWith(others: Array[Char]): Boolean = {

    if (others.length > length)
      false

    else {
      var i = 0
      while (i < length && i < others.length) {
        if (charArray(offset + i) != others(i))
          return false
        i += 1
      }

      i == others.length
    }
  }

  def isBlank: Boolean = {
    for (i <- offset until offset + length)
      if (charArray(i) != ' ')
        return false

    true
  }

  override def toString: String = String.valueOf(charArray, offset, length)
}
