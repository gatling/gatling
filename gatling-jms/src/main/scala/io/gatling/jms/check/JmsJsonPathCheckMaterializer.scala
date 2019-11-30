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

package io.gatling.jms.check

import com.fasterxml.jackson.databind.JsonNode
import io.gatling.commons.validation._
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.{ CheckMaterializer, Preparer }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers
import io.gatling.jms.JmsCheck
import io.gatling.jms.check.JmsMessageBodyPreparers.getBodyAsString
import javax.jms.{ BytesMessage, Message, TextMessage }

class JmsJsonPathCheckMaterializer(jsonParsers: JsonParsers, config: GatlingConfiguration)
    extends CheckMaterializer[JsonPathCheckType, JmsCheck, Message, JsonNode](identity) {
  override protected def preparer: Preparer[Message, JsonNode] =
    JmsJsonPathCheckMaterializer.jsonPathPreparer(jsonParsers, config)
}

object JmsJsonPathCheckMaterializer {
  private val ErrorMapper = "Could not parse response into a JSON: " + _

  private def jsonPathPreparer(jsonParsers: JsonParsers, config: GatlingConfiguration): Preparer[Message, JsonNode] =
    replyMessage =>
      safely(ErrorMapper) {
        replyMessage match {
          case tm: TextMessage  => jsonParsers.safeParse(tm.getText)
          case bm: BytesMessage => jsonParsers.safeParse(getBodyAsString(bm, config))
          case _                => "Unsupported message type".failure
        }
      }
}
