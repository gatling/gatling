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
package io.gatling.jms.check

import java.io.StringReader
import javax.jms.{ Message, TextMessage }

import io.gatling.commons.validation._
import io.gatling.core.check._
import io.gatling.core.check.extractor.xpath._
import io.gatling.jms.JmsCheck
import org.xml.sax.InputSource

object JmsXPathCheckBuilder extends XPathCheckBuilder[JmsCheck, Message] {

  private val ErrorMapper: String => String = "Could not parse response into a DOM Document: " + _

  def preparer[T](f: InputSource => T)(message: Message): Validation[Option[T]] =
    safely(ErrorMapper) {
      message match {
        case tm: TextMessage => Some(f(new InputSource(new StringReader(tm.getText)))).success
        case _               => "Unsupported message type".failure
      }
    }

  val CheckBuilder: Extender[JmsCheck, Message] = (wrapped: JmsCheck) => wrapped
}
