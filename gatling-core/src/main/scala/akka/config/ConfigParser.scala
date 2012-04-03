/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package akka.config

import scala.collection.mutable
import scala.util.parsing.combinator._

class ConfigParser(var prefix: String = "", map: mutable.Map[String, Any] = mutable.Map.empty[String, Any], importer: Importer) extends RegexParsers {
  val sections = mutable.Stack[String]()

  def createPrefix = {
    prefix = if (sections.isEmpty) "" else sections.toList.reverse.mkString("", ".", ".")
  }

  override val whiteSpace = """(\s+|#[^\n]*\n)+""".r

  // tokens

  val numberToken: Parser[String] = """-?\d+(\.\d+)?""".r
  val stringToken: Parser[String] = ("\"" + """([^\\\"]|\\[^ux]|\\\n|\\u[0-9a-fA-F]{4}|\\x[0-9a-fA-F]{2})*""" + "\"").r
  val booleanToken: Parser[String] = "(true|on|false|off)".r
  val identToken: Parser[String] = """([\da-zA-Z_][-\w]*)(\.[a-zA-Z_][-\w]*)*""".r
  val assignToken: Parser[String] = "=".r
  val sectionToken: Parser[String] = """[a-zA-Z][-\w]*""".r

  // values

  def value: Parser[Any] = number | string | list | boolean
  def number = numberToken
  def string = stringToken ^^ { s => s.substring(1, s.length - 1) }
  def list = "[" ~> repsep(string | numberToken, opt(",")) <~ (opt(",") ~ "]")
  def boolean = booleanToken

  // parser

  def root = rep(includeFile | assignment | sectionOpen | sectionClose)

  def includeFile = "include" ~> string ^^ {
    case filename: String =>
      new ConfigParser(prefix, map, importer) parse importer.importFile(filename)
  }

  def assignment = identToken ~ assignToken ~ value ^^ {
    case k ~ a ~ v => map(prefix + k) = v
  }

  def sectionOpen = sectionToken <~ "{" ^^ { name =>
    sections push name
    createPrefix
  }

  def sectionClose = "}" ^^ { _ =>
    if (sections.isEmpty) {
      failure("dangling close tag")
    } else {
      sections.pop
      createPrefix
    }
  }

  def parse(in: String): Map[String, Any] = {
    parseAll(root, in) match {
      case Success(result, _) => map.toMap
      case x@Failure(msg, _)  => throw new ConfigurationException(x.toString)
      case x@Error(msg, _)    => throw new ConfigurationException(x.toString)
    }
  }
}
