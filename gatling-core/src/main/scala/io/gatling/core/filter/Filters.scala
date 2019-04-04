/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.core.filter

import scala.util.matching.Regex
import scala.util.{ Failure, Success, Try }

import com.typesafe.scalalogging.StrictLogging

object Filters {
  val BrowserNoiseFilters =
    Filters(
      BlackList(
        Seq(
          ".*/detectportal.firefox.com/.*",
          ".*/incoming.telemetry.mozilla.org/.*",
          ".*/safebrowsing.googleapis.com/.*",
          ".*/search.services.mozilla.com/.*",
          ".*/snippets.cdn.mozilla.net/.*",
          ".*/tiles.services.mozilla.com/.*",
          ".*/shavar.services.mozilla.com/.*",
          ".*/tracking-protection.cdn.mozilla.net/.*"
        )
      ),
      WhiteList(Seq.empty)
    )
}

case class Filters(first: Filter, second: Filter) {
  def accept(url: String): Boolean = first.accept(url) && second.accept(url)
}

sealed abstract class Filter(patterns: Seq[String]) extends StrictLogging {
  val regexes: Vector[Regex] = patterns.flatMap { p =>
    Try(p.r) match {
      case Success(regex) => Some(regex)
      case Failure(t) =>
        logger.error(s"""Incorrect filter pattern "$p": ${t.getMessage}""")
        None
    }
  }.toVector
  def accept(url: String): Boolean
}

case class WhiteList(patterns: Seq[String] = Nil) extends Filter(patterns) {
  def accept(url: String): Boolean = regexes.isEmpty || regexes.exists(_.pattern.matcher(url).matches)
}

case class BlackList(patterns: Seq[String] = Nil) extends Filter(patterns) {
  def accept(url: String): Boolean = regexes.forall(!_.pattern.matcher(url).matches)
}
