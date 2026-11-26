/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.render

import java.{ lang => jl }

import io.gatling.commons.util.StringHelper.Eol

package object template {
  val SimpleQuotes: String = "\""
  val TripleQuotes: String = SimpleQuotes * 3

  private def isUnsafeStringChar(char: Char) = char == '\\' || char == '"' || char == '\n'

  private[template] implicit final class TemplateString(val string: String) extends AnyVal {
    def protect(format: RenderingFormat): String =
      format match {
        case RenderingFormat.Scala  => protectScalaString
        case RenderingFormat.Kotlin => protectEscapedString(escapeDollar = true)
        case _                      => protectEscapedString(escapeDollar = false)
      }

    // triple quotes are more readable but can't contain triple quotes
    // and would still process unicode escapes (deprecated)
    private def canUseTripleQuotes: Boolean =
      !string.contains(TripleQuotes) && !string.contains("\\u") && !string.contains('\r')

    private def protectScalaString: String =
      if (string.exists(isUnsafeStringChar) && canUseTripleQuotes) {
        s"$TripleQuotes$string$TripleQuotes"
      } else {
        protectEscapedString(escapeDollar = false)
      }

    // valid for Java, Kotlin, Scala, JavaScript and TypeScript simple quotes String literals
    private def protectEscapedString(escapeDollar: Boolean): String = {
      val sb = new jl.StringBuilder().append(SimpleQuotes)
      string.foreach {
        case '\\'                => sb.append("\\\\")
        case '"'                 => sb.append("\\\"")
        case '\n'                => sb.append("\\n")
        case '\r'                => sb.append("\\r")
        case '\t'                => sb.append("\\t")
        case '$' if escapeDollar => sb.append("\\$")
        case char                => sb.append(char)
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
