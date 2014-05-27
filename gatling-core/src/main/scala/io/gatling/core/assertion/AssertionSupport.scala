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
package io.gatling.core.assertion

import scala.reflect.io.Path

import io.gatling.core.result.{ GroupStatsPath, RequestStatsPath }
import io.gatling.core.result.message.Status
import io.gatling.core.result.reader.{ DataReader, GeneralStats }
import io.gatling.core.validation._

trait AssertionSupport {

  val global = new Selector((reader, status) => reader.requestGeneralStats(None, None, status).success, "Global")

  def details(selector: Path): Selector = {

      def generalStats(selector: Path): (DataReader, Option[Status]) => Validation[GeneralStats] = (reader, status) =>
        if (selector.segments.isEmpty)
          reader.requestGeneralStats(None, None, status).success

        else {
          val selectedPath: List[String] = selector.segments
          val foundPath = reader.statsPaths.find { statsPath =>
            val path: List[String] = statsPath match {
              case RequestStatsPath(request, group) =>
                group match {
                  case Some(g) => g.hierarchy :+ request
                  case _       => List(request)
                }
              case GroupStatsPath(group) => group.hierarchy
            }
            path == selectedPath
          }

          foundPath match {
            case None                                   => s"Could not find stats matching selector $selector".failure
            case Some(RequestStatsPath(request, group)) => reader.requestGeneralStats(Some(request), group, status).success
            case Some(GroupStatsPath(group))            => reader.requestGeneralStats(None, Some(group), status).success
          }
        }

    new Selector(generalStats(selector), selector.segments.mkString(" / "))
  }
}
