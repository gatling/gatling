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

package io.gatling.commons.util

import java.util.ResourceBundle

import scala.collection.JavaConverters._

object HtmlHelper {

  private val entities = ResourceBundle.getBundle("html-entities")

  private val charToHtmlEntities: Map[Char, String] = entities.getKeys.asScala.map { entityName =>
    (entities.getString(entityName).toInt.toChar, s"&$entityName;")
  }.toMap

  implicit class HtmlRichString(val string: String) extends AnyVal {

    def htmlEscape: String =
      string.map(char => charToHtmlEntities.getOrElse(char, char.toString)).mkString
  }
}