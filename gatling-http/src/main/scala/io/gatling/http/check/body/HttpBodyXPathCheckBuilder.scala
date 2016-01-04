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
package io.gatling.http.check.body

import io.gatling.commons.validation._
import io.gatling.core.check._
import io.gatling.core.check.extractor.xpath.XPathCheckBuilder
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response
import org.xml.sax.InputSource

object HttpBodyXPathCheckBuilder extends XPathCheckBuilder[HttpCheck, Response] {

  private val ErrorMapper = "Could not parse response into a DOM Document: " + _

  def preparer[T](f: InputSource => T)(response: Response): Validation[Option[T]] =
    safely(ErrorMapper) {
      val root = if (response.hasResponseBody) Some(f(new InputSource(response.body.stream))) else None
      root.success
    }

  val CheckBuilder: Extender[HttpCheck, Response] = StreamBodyExtender
}
