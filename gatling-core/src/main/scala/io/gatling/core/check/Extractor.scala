/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.core.check

import io.gatling.commons.validation.Validation

trait Extractor[P, X] {
  def name: String
  def arity: String
  def apply(prepared: P): Validation[Option[X]]
}

class FindExtractor[P, X](val name: String, extractor: P => Validation[Option[X]]) extends Extractor[P, X] {
  override def apply(prepared: P): Validation[Option[X]] = extractor(prepared)
  override val arity = "find"
}

abstract class CriterionExtractor[P, T, X](checkName: String, criterion: T) extends Extractor[P, X] {
  override val name = s"$checkName($criterion)"
}

class FindCriterionExtractor[P, T, X](checkName: String, criterion: T, occurrence: Int, extractor: P => Validation[Option[X]])
    extends CriterionExtractor[P, T, X](checkName, criterion) {
  override def apply(prepared: P): Validation[Option[X]] = extractor(prepared)
  override val arity: String = if (occurrence == 0) "find" else s"find($occurrence)"
}

class FindAllCriterionExtractor[P, T, X](checkName: String, criterion: T, extractor: P => Validation[Option[Seq[X]]])
    extends CriterionExtractor[P, T, Seq[X]](checkName, criterion) {
  override def apply(prepared: P): Validation[Option[Seq[X]]] = extractor(prepared)
  override val arity = "findAll"
}

class CountCriterionExtractor[P, T](checkName: String, criterion: T, extractor: P => Validation[Option[Int]])
    extends CriterionExtractor[P, T, Int](checkName, criterion) {
  override def apply(prepared: P): Validation[Option[Int]] = extractor(prepared)
  override val arity = "count"
}
