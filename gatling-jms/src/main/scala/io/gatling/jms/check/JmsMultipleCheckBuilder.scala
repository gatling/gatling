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
package io.gatling.jms.check

import io.gatling.core.check.{ ValidatorCheckBuilder, ExtractorCheckBuilder, CheckFactory, Preparer }
import javax.jms.Message
import io.gatling.jms.JmsCheck
import io.gatling.core.check.extractor.Extractor
import io.gatling.core.session.Expression

abstract class JmsMultipleCheckBuilder[P, X](
    val checkFactory: CheckFactory[JmsCheck, Message],
    val preparer: Preparer[Message, P]) extends ExtractorCheckBuilder[JmsCheck, Message, P, X] {

  def findExtractor(occurrence: Int): Expression[Extractor[P, X]]
  def findAllExtractor: Expression[Extractor[P, Seq[X]]]
  def countExtractor: Expression[Extractor[P, Int]]

  def find = find(0)
  def find(occurrence: Int): ValidatorCheckBuilder[JmsCheck, Message, P, X] = ValidatorCheckBuilder(checkFactory, preparer, findExtractor(occurrence))
  def findAll: ValidatorCheckBuilder[JmsCheck, Message, P, Seq[X]] = ValidatorCheckBuilder(checkFactory, preparer, findAllExtractor)
  def count: ValidatorCheckBuilder[JmsCheck, Message, P, Int] = ValidatorCheckBuilder(checkFactory, preparer, countExtractor)
}
