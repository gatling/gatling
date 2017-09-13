/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.validation._
import io.gatling.core.check.extractor.Extractor
import io.gatling.core.session._

@deprecated("Only used in old Async checks, will be replaced with new impl, will be removed in 3.0.0", "3.0.0-M1")
trait OldFindCheckBuilder[C <: Check[R], R, P, X] {

  def find: OldValidatorCheckBuilder[C, R, P, X]
}

@deprecated("Only used in old Async checks, will be replaced with new impl, will be removed in 3.0.0", "3.0.0-M1")
class OldDefaultFindCheckBuilder[C <: Check[R], R, P, X](
    specializer: Specializer[C, R],
    preparer:    Preparer[R, P],
    extractor:   Expression[Extractor[P, X]]
)
  extends OldFindCheckBuilder[C, R, P, X] {

  def find: OldValidatorCheckBuilder[C, R, P, X] = OldValidatorCheckBuilder(specializer, preparer, extractor)
}

@deprecated("Only used in old Async checks, will be replaced with new impl, will be removed in 3.0.0", "3.0.0-M1")
trait OldMultipleFindCheckBuilder[C <: Check[R], R, P, X] extends OldFindCheckBuilder[C, R, P, X] {

  def find(occurrence: Int): OldValidatorCheckBuilder[C, R, P, X]

  def findAll: OldValidatorCheckBuilder[C, R, P, Seq[X]]

  def count: OldValidatorCheckBuilder[C, R, P, Int]
}

@deprecated("Only used in old Async checks, will be replaced with new impl, will be removed in 3.0.0", "3.0.0-M1")
abstract class OldDefaultMultipleFindCheckBuilder[C <: Check[R], R, P, X](
    specializer: Specializer[C, R],
    preparer:    Preparer[R, P]
)
  extends OldMultipleFindCheckBuilder[C, R, P, X] {

  def findExtractor(occurrence: Int): Expression[Extractor[P, X]]

  def findAllExtractor: Expression[Extractor[P, Seq[X]]]

  def countExtractor: Expression[Extractor[P, Int]]

  def find = find(0)

  def find(occurrence: Int): OldValidatorCheckBuilder[C, R, P, X] = OldValidatorCheckBuilder(specializer, preparer, findExtractor(occurrence))

  def findAll: OldValidatorCheckBuilder[C, R, P, Seq[X]] = OldValidatorCheckBuilder(specializer, preparer, findAllExtractor)

  def count: OldValidatorCheckBuilder[C, R, P, Int] = OldValidatorCheckBuilder(specializer, preparer, countExtractor)
}

@deprecated("Only used in old Async checks, will be replaced with new impl, will be removed in 3.0.0", "3.0.0-M1")
object OldValidatorCheckBuilder {
  val TransformErrorMapper: String => String = "transform crashed: " + _
  val TransformOptionErrorMapper: String => String = "transformOption crashed: " + _
}

@deprecated("Only used in old Async checks, will be replaced with new impl, will be removed in 3.0.0", "3.0.0-M1")
case class OldValidatorCheckBuilder[C <: Check[R], R, P, X](
    specializer: Specializer[C, R],
    preparer:    Preparer[R, P],
    extractor:   Expression[Extractor[P, X]]
) {

  import OldValidatorCheckBuilder._

  private def transformExtractor[X2](transformation: X => X2)(extractor: Extractor[P, X]) =
    new Extractor[P, X2] {
      def name = extractor.name
      def arity = extractor.arity + ".transform"

      def apply(prepared: P): Validation[Option[X2]] =
        safely(TransformErrorMapper) {
          extractor(prepared).map(_.map(transformation))
        }
    }

  def transform[X2](transformation: X => X2): OldValidatorCheckBuilder[C, R, P, X2] =
    copy(extractor = extractor.map(transformExtractor(transformation)))

  def transform[X2](transformation: (X, Session) => X2): OldValidatorCheckBuilder[C, R, P, X2] =
    copy(extractor = session => extractor(session).map(transformExtractor(transformation(_, session))))

  private def transformOptionExtractor[X2](transformation: Option[X] => Validation[Option[X2]])(extractor: Extractor[P, X]) =
    new Extractor[P, X2] {
      def name = extractor.name
      def arity = extractor.arity + ".transformOption"

      def apply(prepared: P): Validation[Option[X2]] =
        safely(TransformOptionErrorMapper) {
          extractor(prepared).flatMap(transformation)
        }
    }

  def transformOption[X2](transformation: Option[X] => Validation[Option[X2]]): OldValidatorCheckBuilder[C, R, P, X2] =
    copy(extractor = extractor.map(transformOptionExtractor(transformation)))

  def transformOption[X2](transformation: (Option[X], Session) => Validation[Option[X2]]): OldValidatorCheckBuilder[C, R, P, X2] =
    copy(extractor = session => extractor(session).map(transformOptionExtractor(transformation(_, session))))

  def validate(validator: Expression[Validator[X]]): OldCheckBuilder[C, R, P, X] with OldSaveAs[C, R, P, X] =
    new OldCheckBuilder(this, validator) with OldSaveAs[C, R, P, X]

  def validate(opName: String, validator: (Option[X], Session) => Validation[Option[X]]): OldCheckBuilder[C, R, P, X] with OldSaveAs[C, R, P, X] =
    validate((session: Session) => new Validator[X] {
      def name = opName
      def apply(actual: Option[X]): Validation[Option[X]] = validator(actual, session)
    }.success)

  def is(expected: Expression[X]) = validate(expected.map(new IsMatcher(_)))
  def not(expected: Expression[X]) = validate(expected.map(new NotMatcher(_)))
  def in(expected: X*) = validate(expected.toSeq.expressionSuccess.map(new InMatcher(_)))
  def in(expected: Expression[Seq[X]]) = validate(expected.map(new InMatcher(_)))
  def exists = validate(new ExistsValidator[X]().expressionSuccess)
  def notExists = validate(new NotExistsValidator[X]().expressionSuccess)
  def optional = validate(new NoopValidator[X]().expressionSuccess)
  def lessThan(expected: Expression[X])(implicit ordering: Ordering[X]) = validate(expected.map(new CompareMatcher("lessThan", "less than", ordering.lt, _)))
  def lessThanOrEqual(expected: Expression[X])(implicit ordering: Ordering[X]) = validate(expected.map(new CompareMatcher("lessThanOrEqual", "less than or equal to", ordering.lteq, _)))
  def greaterThan(expected: Expression[X])(implicit ordering: Ordering[X]) = validate(expected.map(new CompareMatcher("greaterThan", "greater than", ordering.gt, _)))
  def greaterThanOrEqual(expected: Expression[X])(implicit ordering: Ordering[X]) = validate(expected.map(new CompareMatcher("greaterThanOrEqual", "greater than or equal to", ordering.gteq, _)))
}

@deprecated("Only used in old Async checks, will be replaced with new impl, will be removed in 3.0.0", "3.0.0-M1")
case class OldCheckBuilder[C <: Check[R], R, P, X](
    validatorCheckBuilder: OldValidatorCheckBuilder[C, R, P, X],
    validator:             Expression[Validator[X]],
    saveAs:                Option[String]                       = None
) {

  def build: C = {
    val base = CheckBase(validatorCheckBuilder.preparer, validatorCheckBuilder.extractor, validator, saveAs)
    validatorCheckBuilder.specializer(base)
  }
}

@deprecated("Only used in old Async checks, will be replaced with new impl, will be removed in 3.0.0", "3.0.0-M1")
trait OldSaveAs[C <: Check[R], R, P, X] { this: OldCheckBuilder[C, R, P, X] =>
  def saveAs(key: String): OldCheckBuilder[C, R, P, X] = copy(saveAs = Some(key))
}
