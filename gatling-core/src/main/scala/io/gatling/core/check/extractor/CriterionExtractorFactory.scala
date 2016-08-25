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

abstract class CriterionExtractorFactory[P, T](name: String) {

  def newSingleExtractor[X](_criterion: T, _occurrence: Int, extractor: P => Validation[Option[X]]) =
    new CriterionExtractor[P, T, X] with FindArity {

      override def criterionName = CriterionExtractorFactory.this.name

      override def criterion = _criterion

      override def occurrence = _occurrence

      override def apply(prepared: P): Validation[Option[X]] = extractor(prepared)
    }

  def newMultipleExtractor[X](_criterion: T, extractor: P => Validation[Option[Seq[X]]]) =
    new CriterionExtractor[P, T, Seq[X]] with FindAllArity {

      override def criterionName = CriterionExtractorFactory.this.name

      override def criterion = _criterion

      override def apply(prepared: P): Validation[Option[Seq[X]]] = extractor(prepared)
    }

  def newCountExtractor(_criterion: T, extractor: P => Validation[Option[Int]]) =
    new CriterionExtractor[P, T, Int] with CountArity {

      override def criterionName = CriterionExtractorFactory.this.name

      override def criterion = _criterion

      override def apply(prepared: P): Validation[Option[Int]] = extractor(prepared)
    }
}
