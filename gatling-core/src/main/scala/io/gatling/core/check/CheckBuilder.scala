/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import java.util.concurrent.ThreadLocalRandom

import io.gatling.commons.util.ThreadLocalRandoms
import io.gatling.commons.validation._
import io.gatling.core.check.extractor.Extractor
import io.gatling.core.session._

// T: Check type, only used for CheckMaterializer Type Class
// P: Prepared
// X: Extracted
trait FindCheckBuilder[T, P, X] {

  def find: ValidatorCheckBuilder[T, P, X]
}

class DefaultFindCheckBuilder[T, P, X](extractor: Expression[Extractor[P, X]], displayActualValue: Boolean)
  extends FindCheckBuilder[T, P, X] {

  def find: ValidatorCheckBuilder[T, P, X] = ValidatorCheckBuilder(extractor, displayActualValue)
}

trait MultipleFindCheckBuilder[T, P, X] extends FindCheckBuilder[T, P, X] {

  override def find: ValidatorCheckBuilder[T, P, X]

  def find(occurrence: Int): ValidatorCheckBuilder[T, P, X]

  def findAll: ValidatorCheckBuilder[T, P, Seq[X]]

  def findRandom: ValidatorCheckBuilder[T, P, X]

  def findRandom(num: Int, failIfLess: Boolean = false): ValidatorCheckBuilder[T, P, Seq[X]]

  def count: ValidatorCheckBuilder[T, P, Int]
}

abstract class DefaultMultipleFindCheckBuilder[T, P, X](displayActualValue: Boolean)
  extends MultipleFindCheckBuilder[T, P, X] {

  def findExtractor(occurrence: Int): Expression[Extractor[P, X]]

  def findAllExtractor: Expression[Extractor[P, Seq[X]]]

  def findRandomExtractor: Expression[Extractor[P, X]] = findAllExtractor.map { fae =>

    new Extractor[P, X] {
      override def name: String = fae.name
      override def arity: String = "findRandom"

      override def apply(prepared: P): Validation[Option[X]] =
        fae(prepared)
          .map(_.collect { case seq if seq.nonEmpty => seq(ThreadLocalRandom.current.nextInt(seq.size)) })
    }
  }

  def findManyRandomExtractor(num: Int, failIfLess: Boolean): Expression[Extractor[P, Seq[X]]] = findAllExtractor.map { fae =>

    new Extractor[P, Seq[X]] {
      override def name: String = fae.name
      override def arity: String = s"findRandom($num, $failIfLess)"

      override def apply(prepared: P): Validation[Option[Seq[X]]] =
        fae(prepared)
          .flatMap {
            case Some(seq) =>
              if (failIfLess && seq.size < num) {
                s"Failed to collect $num matches".failure

              } else if (seq.isEmpty) {
                NoneSuccess

              } else {
                val randomSeq =
                  if (num >= seq.size) {
                    seq
                  } else {
                    val sortedRandomIndexes = ThreadLocalRandoms.shuffle(seq.indices.toVector).take(num).sorted
                    sortedRandomIndexes.map(seq)
                  }

                Some(randomSeq).success
              }

            case None => NoneSuccess
          }
    }
  }

  def countExtractor: Expression[Extractor[P, Int]]

  def find: ValidatorCheckBuilder[T, P, X] = find(0)

  def find(occurrence: Int): ValidatorCheckBuilder[T, P, X] = ValidatorCheckBuilder(findExtractor(occurrence), displayActualValue)

  def findAll: ValidatorCheckBuilder[T, P, Seq[X]] = ValidatorCheckBuilder(findAllExtractor, displayActualValue)

  def findRandom: ValidatorCheckBuilder[T, P, X] = ValidatorCheckBuilder(findRandomExtractor, displayActualValue)

  def findRandom(num: Int, failIfLess: Boolean): ValidatorCheckBuilder[T, P, Seq[X]] = ValidatorCheckBuilder(findManyRandomExtractor(num, failIfLess), displayActualValue)

  def count: ValidatorCheckBuilder[T, P, Int] = ValidatorCheckBuilder(countExtractor, displayActualValue)
}

object ValidatorCheckBuilder {
  val TransformErrorMapper: String => String = "transform crashed: " + _
  val TransformOptionErrorMapper: String => String = "transformOption crashed: " + _
}

case class ValidatorCheckBuilder[T, P, X](extractor: Expression[Extractor[P, X]], displayActualValue: Boolean) {

  import ValidatorCheckBuilder._

  private def transformExtractor[X2](transformation: X => X2)(extractor: Extractor[P, X]) =
    new Extractor[P, X2] {
      override def name: String = extractor.name
      override def arity: String = extractor.arity + ".transform"

      override def apply(prepared: P): Validation[Option[X2]] =
        safely(TransformErrorMapper) {
          extractor(prepared).map(_.map(transformation))
        }
    }

  def transform[X2](transformation: X => X2): ValidatorCheckBuilder[T, P, X2] =
    copy(extractor = extractor.map(transformExtractor(transformation)))

  def transform[X2](transformation: (X, Session) => X2): ValidatorCheckBuilder[T, P, X2] =
    copy(extractor = session => extractor(session).map(transformExtractor(transformation(_, session))))

  private def transformOptionExtractor[X2](transformation: Option[X] => Validation[Option[X2]])(extractor: Extractor[P, X]) =
    new Extractor[P, X2] {
      override def name: String = extractor.name
      override def arity: String = extractor.arity + ".transformOption"

      override def apply(prepared: P): Validation[Option[X2]] =
        safely(TransformOptionErrorMapper) {
          extractor(prepared).flatMap(transformation)
        }
    }

  def transformOption[X2](transformation: Option[X] => Validation[Option[X2]]): ValidatorCheckBuilder[T, P, X2] =
    copy(extractor = extractor.map(transformOptionExtractor(transformation)))

  def transformOption[X2](transformation: (Option[X], Session) => Validation[Option[X2]]): ValidatorCheckBuilder[T, P, X2] =
    copy(extractor = session => extractor(session).map(transformOptionExtractor(transformation(_, session))))

  def validate(validator: Expression[Validator[X]]): CheckBuilder[T, P, X] with SaveAs[T, P, X] =
    new CheckBuilder[T, P, X](this.extractor, validator, displayActualValue) with SaveAs[T, P, X]

  def validate(opName: String, validator: (Option[X], Session) => Validation[Option[X]]): CheckBuilder[T, P, X] with SaveAs[T, P, X] =
    validate((session: Session) => new Validator[X] {
      override val name: String = opName
      override def apply(actual: Option[X], displayActualValue: Boolean): Validation[Option[X]] = validator(actual, session)
    }.success)

  def is(expected: Expression[X]): CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(expected.map(new IsMatcher(_)))
  def isNull: CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(new IsNullMatcher[X].expressionSuccess)
  def not(expected: Expression[X]): CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(expected.map(new NotMatcher(_)))
  def notNull: CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(new NotNullMatcher[X].expressionSuccess)
  def in(expected: X*): CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(expected.toSeq.expressionSuccess.map(new InMatcher(_)))
  def in(expected: Expression[Seq[X]]): CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(expected.map(new InMatcher(_)))
  def exists: CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(new ExistsValidator[X]().expressionSuccess)
  def notExists: CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(new NotExistsValidator[X]().expressionSuccess)
  def optional: CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(new NoopValidator[X]().expressionSuccess)
  def lt(expected: Expression[X])(implicit ordering: Ordering[X]): CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(expected.map(new CompareMatcher("lessThan", "less than", ordering.lt, _)))
  def lte(expected: Expression[X])(implicit ordering: Ordering[X]): CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(expected.map(new CompareMatcher("lessThanOrEqual", "less than or equal to", ordering.lteq, _)))
  def gt(expected: Expression[X])(implicit ordering: Ordering[X]): CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(expected.map(new CompareMatcher("greaterThan", "greater than", ordering.gt, _)))
  def gte(expected: Expression[X])(implicit ordering: Ordering[X]): CheckBuilder[T, P, X] with SaveAs[T, P, X] = validate(expected.map(new CompareMatcher("greaterThanOrEqual", "greater than or equal to", ordering.gteq, _)))
}

case class CheckBuilder[T, P, X](
    extractor:          Expression[Extractor[P, X]],
    validator:          Expression[Validator[X]],
    displayActualValue: Boolean,
    customName:         Option[String]              = None,
    saveAs:             Option[String]              = None
) {
  def name(n: String): CheckBuilder[T, P, X] = copy(customName = Some(n))

  def build[C <: Check[R], R](materializer: CheckMaterializer[T, C, R, P]): C =
    materializer.materialize(this)
}

trait SaveAs[C, P, X] { this: CheckBuilder[C, P, X] =>
  def saveAs(key: String): CheckBuilder[C, P, X] = copy(saveAs = Some(key))
}
