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

import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.check._
import io.gatling.core.check.extractor.xpath.XPathCheckBuilder
import io.gatling.core.validation._
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response
import org.xml.sax.InputSource

object HttpBodyXPathCheckBuilder extends XPathCheckBuilder[HttpCheck, Response] with StrictLogging {

  def preparer[T](f: InputSource => T)(response: Response): Validation[Option[T]] =
    try {
      val root = if (response.hasResponseBody) Some(f(new InputSource(response.body.stream))) else None
      root.success

    } catch {
      case e: Exception =>
        val message = s"Could not parse response into a DOM Document: ${e.getMessage}"
        logger.info(message, e)
        message.failure
    }

  val CheckBuilder: Extender[HttpCheck, Response] = StreamBodyExtender
}
