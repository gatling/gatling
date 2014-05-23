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
package io.gatling.http.check.body

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.check.{ DefaultMultipleFindCheckBuilder, Preparer }
import io.gatling.core.check.extractor.jsonpath.{ BoonParser, CountJsonPathExtractor, JacksonParser, JsonFilter, MultipleJsonPathExtractor, SingleJsonPathExtractor }
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.core.util.StringHelper.{ DirectCharsBasedStringImplementation, stringImplementation }
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper }
import io.gatling.http.check.{ HttpCheck, HttpCheckBuilders }
import io.gatling.http.response.{ ByteArrayResponseBodyUsage, InputStreamResponseBodyUsage, Response, ResponseBodyUsageStrategy, StringResponseBodyUsage }

trait HttpBodyJsonPathOfType {
  self: HttpBodyJsonPathCheckBuilder[String] =>

  def ofType[X](implicit jsonFilter: JsonFilter[X]) = new HttpBodyJsonPathCheckBuilder[X](path)
}

object HttpBodyJsonPathCheckBuilder extends StrictLogging {

  val CharsParsingThreshold = 1000000

  def handleParseException[R](block: R => Any) = (response: R) =>
    try {
      block(response).success
    } catch {
      case e: Exception =>
        val message = s"Could not parse response into a JSON object: ${e.getMessage}"
        logger.info(message, e)
        message.failure
    }

  val Preparer: Preparer[Response, Any] = stringImplementation match {

    case DirectCharsBasedStringImplementation =>
      handleParseException { response =>
        if (response.bodyLength <= CharsParsingThreshold)
          BoonParser.parse(response.body.string)
        else
          BoonParser.parse(response.body.stream, response.charset)
      }

    case _ =>
      handleParseException { response =>
        if (response.bodyLength <= CharsParsingThreshold)
          JacksonParser.parse(response.body.bytes, response.charset)
        else
          JacksonParser.parse(response.body.stream, response.charset)
      }
  }

  val BoonResponseBodyUsageStrategy = new ResponseBodyUsageStrategy {
    def bodyUsage(bodyLength: Int) =
      if (bodyLength <= CharsParsingThreshold)
        StringResponseBodyUsage
      else
        InputStreamResponseBodyUsage
  }

  val JacksonResponseBodyUsageStrategy = new ResponseBodyUsageStrategy {
    def bodyUsage(bodyLength: Int) =
      if (bodyLength <= CharsParsingThreshold)
        ByteArrayResponseBodyUsage
      else
        InputStreamResponseBodyUsage
  }

  val ResponseBodyUsageStrategy = stringImplementation match {
    case DirectCharsBasedStringImplementation => BoonResponseBodyUsageStrategy
    case _                                    => JacksonResponseBodyUsageStrategy
  }

  def jsonPath(path: Expression[String]) = new HttpBodyJsonPathCheckBuilder[String](path) with HttpBodyJsonPathOfType
}

class HttpBodyJsonPathCheckBuilder[X](private[body] val path: Expression[String])(implicit jsonFilter: JsonFilter[X])
    extends DefaultMultipleFindCheckBuilder[HttpCheck, Response, Any, X](
      HttpCheckBuilders.bodyCheckFactory(HttpBodyJsonPathCheckBuilder.ResponseBodyUsageStrategy),
      HttpBodyJsonPathCheckBuilder.Preparer) {

  def findExtractor(occurrence: Int) = path.map(new SingleJsonPathExtractor(_, occurrence))
  def findAllExtractor = path.map(new MultipleJsonPathExtractor(_))
  def countExtractor = path.map(new CountJsonPathExtractor(_))
}
