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
package io.gatling.http.check.ws

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.check.extractor.jsonpath._
import io.gatling.core.check.{ CheckFactory, DefaultMultipleFindCheckBuilder, Preparer }
import io.gatling.core.session.Expression
import io.gatling.http.check.body.HttpBodyJsonpJsonPathCheckBuilder

trait WsJsonpJsonPathOfType {
  self: WsJsonpJsonPathCheckBuilder[String] =>

  def ofType[X: JsonFilter] = new WsJsonpJsonPathCheckBuilder[X](path, checkFactory)
}

object WsJsonpJsonPathCheckBuilder extends StrictLogging {

  val WsJsonpPreparer: Preparer[String, Any] = HttpBodyJsonpJsonPathCheckBuilder.parseJsonpString

  def jsonpJsonPath(path: Expression[String], checkFactory: CheckFactory[WsCheck, String]) =
    new WsJsonpJsonPathCheckBuilder[String](path, checkFactory) with WsJsonpJsonPathOfType
}

class WsJsonpJsonPathCheckBuilder[X: JsonFilter](private[ws] val path: Expression[String],
                                                 private[ws] val checkFactory: CheckFactory[WsCheck, String])
    extends DefaultMultipleFindCheckBuilder[WsCheck, String, Any, X](checkFactory,
      WsJsonpJsonPathCheckBuilder.WsJsonpPreparer) {

  def findExtractor(occurrence: Int) = path.map(new SingleJsonPathExtractor(_, occurrence))

  def findAllExtractor = path.map(new MultipleJsonPathExtractor(_))

  def countExtractor = path.map(new CountJsonPathExtractor(_))
}
