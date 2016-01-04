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
package io.gatling.core.check.extractor

import io.gatling.commons.validation.Validation

abstract class SingleExtractor[P, T, X] {
  def extract(prepared: P, criterion: T, occurrence: Int): Validation[Option[X]]
}

abstract class MultipleExtractor[P, T, X] {
  def extract(prepared: P, criterion: T): Validation[Option[Seq[X]]]
}

abstract class CountExtractor[P, T] {
  def extract(prepared: P, criterion: T): Validation[Option[Int]]
}

abstract class CriterionExtractorFactory[P, T](name: String) {

  def newSingleExtractor[X](_criterion: T, _occurrence: Int)(implicit extractor: SingleExtractor[P, T, X]) =
    new CriterionExtractor[P, T, X] with FindArity {

      override def criterionName = CriterionExtractorFactory.this.name

      override def criterion = _criterion

      override def occurrence = _occurrence

      override def extract(prepared: P): Validation[Option[X]] = extractor.extract(prepared, _criterion, _occurrence)
    }

  def newMultipleExtractor[X](_criterion: T)(implicit extractor: MultipleExtractor[P, T, X]) =
    new CriterionExtractor[P, T, Seq[X]] with FindAllArity {

      override def criterionName = CriterionExtractorFactory.this.name

      override def criterion = _criterion

      override def extract(prepared: P): Validation[Option[Seq[X]]] = extractor.extract(prepared, _criterion)
    }

  def newCountExtractor(_criterion: T)(implicit extractor: CountExtractor[P, T]) =
    new CriterionExtractor[P, T, Int] with CountArity {

      override def criterionName = CriterionExtractorFactory.this.name

      override def criterion = _criterion

      override def extract(prepared: P): Validation[Option[Int]] = extractor.extract(prepared, _criterion)
    }
}
