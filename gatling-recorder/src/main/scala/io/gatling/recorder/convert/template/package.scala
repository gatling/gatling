/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.convert

import java.{ lang => jl }

import io.gatling.commons.util.StringHelper.Eol

package object template {

  val SimpleQuotes: String = "\""
  val TripleQuotes: String = SimpleQuotes * 3

  private def isUnsafeStringChar(char: Char) = char == '\\' || char == '"' || char == '\n'

  private[template] implicit final class TemplateString(val string: String) extends AnyVal {
    def protect(format: Format): String =
      format match {
        case Format.Scala | Format.Kotlin | Format.Java17 => multilineString
        case _                                            => protectJavaString
      }

    private def multilineString: String = {
      val stringDelimiter = if (string.exists(isUnsafeStringChar)) TripleQuotes else SimpleQuotes
      s"$stringDelimiter$string$stringDelimiter"
    }

    private def protectJavaString: String = {
      val sb = new jl.StringBuilder().append(SimpleQuotes)
      string.foreach { char =>
        if (isUnsafeStringChar(char)) {
          sb.append('\\')
        }
        sb.append(char)
      }
      sb.append(SimpleQuotes).toString
    }

    def noEmptyLines: String =
      string.linesIterator
        .filter(_.exists(_ != ' '))
        .mkString(Eol)

    def indent(spaces: Int): String =
      if (string.isEmpty) {
        ""
      } else {
        val prefix = " " * spaces
        string.linesIterator
          .map(line => s"$prefix$line")
          .mkString(Eol)
      }
  }
}
