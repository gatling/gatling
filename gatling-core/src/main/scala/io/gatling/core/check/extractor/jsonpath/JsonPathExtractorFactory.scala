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
package io.gatling.core.check.extractor.jsonpath

import io.gatling.commons.validation.Validation
import io.gatling.core.check.extractor._

class JsonPathExtractorFactory(implicit val jsonPaths: JsonPaths) extends CriterionExtractorFactory[Any, String]("jsonPath") {

  implicit def defaultSingleExtractor[X: JsonFilter] = new SingleExtractor[Any, String, X] {

    def extract(prepared: Any, criterion: String, occurrence: Int): Validation[Option[X]] =
      jsonPaths.extractAll(prepared, criterion).map(_.toSeq.lift(occurrence))
  }

  implicit def defaultMultipleExtractor[X: JsonFilter] = new MultipleExtractor[Any, String, X] {
    def extract(prepared: Any, criterion: String): Validation[Option[Seq[X]]] =
      jsonPaths.extractAll(prepared, criterion).map(_.toVector.liftSeqOption)
  }

  implicit val defaultCountExtractor = new CountExtractor[Any, String] {
    def extract(prepared: Any, criterion: String): Validation[Option[Int]] =
      jsonPaths.extractAll[Any](prepared, criterion).map(i => Some(i.size))
  }
}
