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
package io.gatling.http.fetch

import scala.annotation.{ switch, tailrec }
import scala.collection.TraversableOnce.flattenTraversableOnce
import scala.util.matching.Regex

import io.gatling.http.util.HttpHelper

import org.asynchttpclient.uri.Uri
import com.typesafe.scalalogging.StrictLogging

object CssParser extends StrictLogging {

  val InlineStyleImageUrls = """url\((.*)\)""".r
  val StyleImportsUrls = """@import url\((.*)\)""".r

  def extractUrls(string: CharSequence, regex: Regex): Iterator[String] =
    regex.findAllIn(string).matchData.map { m =>
      val raw = m.group(1)
      extractUrl(raw, 0, raw.length)
    }.flatten

  val SingleQuoteEscapeChar = Some('\'')
  val DoubleQuoteEscapeChar = Some('"')
  val AtImportChars = "@import".toCharArray
  val UrlStartChars = "url(".toCharArray

  def extractUrl(string: String, start: Int, end: Int): Option[String] =
    if (string.isEmpty) {
      None
    } else {
      var protectChar: Option[Char] = None
      var broken = false

        @tailrec
        def trimLeft(cur: Int): Int =
          if (cur == end)
            cur
          else
            (string.charAt(cur): @switch) match {
              case ' ' | '\r' | '\n' => trimLeft(cur + 1)
              case '\'' =>
                protectChar match {
                  case None =>
                    protectChar = SingleQuoteEscapeChar
                    trimLeft(cur + 1)
                  case _ =>
                    broken = true
                    cur

                }
              case '"' =>
                protectChar match {
                  case None =>
                    protectChar = DoubleQuoteEscapeChar
                    trimLeft(cur + 1)
                  case _ =>
                    broken = true
                    cur
                }
              case _ => cur
            }

        @tailrec
        def trimRight(cur: Int, leftLimit: Int): Int =
          if (cur == leftLimit)
            cur
          else
            (string.charAt(cur - 1): @switch) match {
              case ' ' | '\r' | '\n' => trimRight(cur - 1, leftLimit)
              case '\'' => protectChar match {
                case `SingleQuoteEscapeChar` =>
                  trimRight(cur - 1, leftLimit)
                case _ =>
                  broken = true
                  cur
              }
              case '"' => protectChar match {
                case `DoubleQuoteEscapeChar` =>
                  trimRight(cur - 1, leftLimit)
                case _ =>
                  broken = true
                  cur
              }
              case _ => cur
            }

      val trimmedStart = trimLeft(start)
      val trimmedEnd = trimRight(end, trimmedStart)

      if (!broken && trimmedStart != trimmedEnd) {
        if (string.charAt(trimmedStart) == '#')
          // anchors are not real urls
          None
        else
          Some(string.substring(trimmedStart, trimmedEnd))
      } else {
        logger.info(s"css url $string broken")
        None
      }
    }

  def extractResources(cssURI: Uri, cssContent: String): List[EmbeddedResource] = {

    val resources = collection.mutable.ArrayBuffer.empty[EmbeddedResource]

    var withinComment = false
    var withinImport = false
    var withinUrl = false
    var urlStart = 0

      def charsMatch(i: Int, chars: Array[Char]): Boolean = {

          @tailrec
          def charsMatchRec(j: Int): Boolean = {
            if (j == chars.length)
              true
            else if (cssContent.charAt(i + j) != chars(j))
              false
            else
              charsMatchRec(j + 1)

          }

        i < cssContent.length - chars.length && charsMatchRec(1)
      }

    var i = 0
    while (i < cssContent.length) {

      (cssContent.charAt(i): @switch) match {
        case '/' =>
          if (i < cssContent.length - 1 &&
            cssContent.charAt(i + 1) == '*') {
            withinComment = true
            i += 1

          } else if (i > 0 &&
            cssContent.charAt(i - 1) == '*') {
            withinComment = false
          }

        case '@' if !withinComment && charsMatch(i, AtImportChars) =>
          withinImport = true
          i = i + "@import".length

        case 'u' if !withinComment && withinImport && charsMatch(i, UrlStartChars) =>
          i = i + UrlStartChars.length
          urlStart = i
          withinUrl = true

        case ')' if !withinComment && withinUrl =>
          for {
            url <- extractUrl(cssContent, urlStart, i)
            absoluteUri <- HttpHelper.resolveFromUriSilently(cssURI, url)
          } {
            resources += CssResource(absoluteUri)
            withinUrl = false
            withinImport = false
          }

        case _ =>
      }

      i += 1
    }

    resources.toList
  }
}
