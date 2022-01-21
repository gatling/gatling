/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util.{ Arrays, Equality }
import io.gatling.commons.validation._
import io.gatling.core.session._

trait CheckBuilder[T, P] {
  def build[C <: Check[R], R](materializer: CheckMaterializer[T, C, R, P]): C
}

object CheckBuilder {
  // T: Check type, only used for CheckMaterializer Type Class
  // P: Prepared
  // X: Extracted
  trait Find[T, P, X] {
    def find: Validate[T, P, X]
  }

  object Find {
    class Default[T, P, X](extractor: Expression[Extractor[P, X]], displayActualValue: Boolean) extends Find[T, P, X] {
      override def find: Validate[T, P, X] = Validate.Default(extractor, displayActualValue)
    }
  }

  trait MultipleFind[T, P, X] extends Find[T, P, X] {
    override def find: Validate[T, P, X]
    def find(occurrence: Int): Validate[T, P, X]
    def findAll: Validate[T, P, Seq[X]]
    def findRandom: Validate[T, P, X]
    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    def findRandom(num: Int, failIfLess: Boolean = false): Validate[T, P, Seq[X]]
    def count: Validate[T, P, Int]
  }

  object MultipleFind {
    abstract class Default[T, P, X](displayActualValue: Boolean) extends MultipleFind[T, P, X] {

      protected def findExtractor(occurrence: Int): Expression[Extractor[P, X]]

      protected def findAllExtractor: Expression[Extractor[P, Seq[X]]]

      private def findRandomExtractor: Expression[Extractor[P, X]] = findAllExtractor.map { fae =>
        new Extractor[P, X] {
          override def name: String = fae.name
          override def arity: String = "findRandom"

          override def apply(prepared: P): Validation[Option[X]] =
            fae(prepared)
              .map(_.collect { case seq if seq.nonEmpty => seq(ThreadLocalRandom.current.nextInt(seq.size)) })
        }
      }

      private def findManyRandomExtractor(num: Int, failIfLess: Boolean): Expression[Extractor[P, Seq[X]]] = findAllExtractor.map { fae =>
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
                    Validation.NoneSuccess

                  } else {
                    val randomSeq =
                      if (num >= seq.size) {
                        seq
                      } else {
                        val shuffledIndices = Arrays.shuffle(seq.indices.toArray)
                        for (i <- 0 until num) yield seq(shuffledIndices(i))
                      }

                    Some(randomSeq).success
                  }

                case _ => Validation.NoneSuccess
              }
        }
      }

      protected def countExtractor: Expression[Extractor[P, Int]]

      override def find: Validate[T, P, X] = find(0)

      override def find(occurrence: Int): Validate[T, P, X] = Validate.Default(findExtractor(occurrence), displayActualValue)

      override def findAll: Validate[T, P, Seq[X]] = Validate.Default(findAllExtractor, displayActualValue)

      override def findRandom: Validate[T, P, X] = Validate.Default(findRandomExtractor, displayActualValue)

      override def findRandom(num: Int, failIfLess: Boolean): Validate[T, P, Seq[X]] =
        Validate.Default(findManyRandomExtractor(num, failIfLess), displayActualValue)

      override def count: Validate[T, P, Int] = Validate.Default(countExtractor, displayActualValue)
    }
  }

  trait Validate[T, P, X] {
    def transform[X2](transformation: X => X2): Validate[T, P, X2]
    def transformWithSession[X2](transformation: (X, Session) => X2): Validate[T, P, X2]
    def transformOption[X2](transformation: Option[X] => Validation[Option[X2]]): Validate[T, P, X2]
    def transformOptionWithSession[X2](transformation: (Option[X], Session) => Validation[Option[X2]]): Validate[T, P, X2]
    def withDefault(defaultValue: Expression[X]): Validate[T, P, X]
    def validate(validator: Expression[Validator[X]]): Final[T, P]
    def validate(opName: String, validator: (Option[X], Session) => Validation[Option[X]]): Final[T, P]
    def is(expected: Expression[X])(implicit equality: Equality[X]): Final[T, P]
    def isNull: Final[T, P]
    def not(expected: Expression[X])(implicit equality: Equality[X]): Final[T, P]
    def notNull: Final[T, P]
    def in(expected: X*): Final[T, P]
    def in(expected: Expression[Seq[X]]): Final[T, P]
    def exists: Final[T, P]
    def notExists: Final[T, P]
    def optional: Final[T, P]
    def lt(expected: Expression[X])(implicit ordering: Ordering[X]): Final[T, P]
    def lte(expected: Expression[X])(implicit ordering: Ordering[X]): Final[T, P]
    def gt(expected: Expression[X])(implicit ordering: Ordering[X]): Final[T, P]
    def gte(expected: Expression[X])(implicit ordering: Ordering[X]): Final[T, P]
  }

  object Validate {
    val TransformErrorMapper: String => String = "transform crashed: " + _
    val TransformOptionErrorMapper: String => String = "transformOption crashed: " + _

    final case class Default[T, P, X](extractor: Expression[Extractor[P, X]], displayActualValue: Boolean) extends Validate[T, P, X] {

      private def transformExtractor[X2](transformation: X => X2)(extractor: Extractor[P, X]) =
        new Extractor[P, X2] {
          override def name: String = extractor.name
          override def arity: String = extractor.arity + ".transform"

          override def apply(prepared: P): Validation[Option[X2]] =
            safely(TransformErrorMapper) {
              extractor(prepared).map(_.map(transformation))
            }
        }

      private def transformOptionExtractor[X2](transformation: Option[X] => Validation[Option[X2]])(extractor: Extractor[P, X]) =
        new Extractor[P, X2] {
          override def name: String = extractor.name
          override def arity: String = extractor.arity + ".transformOption"

          override def apply(prepared: P): Validation[Option[X2]] =
            safely(TransformOptionErrorMapper) {
              extractor(prepared).flatMap(transformation)
            }
        }

      override def transform[X2](transformation: X => X2): Validate[T, P, X2] =
        copy(extractor = extractor.map(transformExtractor(transformation)))

      override def transformWithSession[X2](transformation: (X, Session) => X2): Validate[T, P, X2] =
        copy(extractor = session => extractor(session).map(transformExtractor(transformation(_, session))))

      override def transformOption[X2](transformation: Option[X] => Validation[Option[X2]]): Validate[T, P, X2] =
        copy(extractor = extractor.map(transformOptionExtractor(transformation)))

      override def transformOptionWithSession[X2](transformation: (Option[X], Session) => Validation[Option[X2]]): Validate[T, P, X2] =
        copy(extractor = session => extractor(session).map(transformOptionExtractor(transformation(_, session))))

      override def withDefault(defaultValue: Expression[X]): Validate[T, P, X] =
        transformOptionWithSession((actual, session) =>
          actual match {
            case None => defaultValue(session).map(Option(_))
            case _    => actual.success
          }
        )

      override def validate(validator: Expression[Validator[X]]): Final[T, P] =
        new Final.Default[T, P, X](this.extractor, validator, displayActualValue, None, None)

      override def validate(opName: String, validator: (Option[X], Session) => Validation[Option[X]]): Final[T, P] =
        validate(session =>
          new Validator[X] {
            override val name: String = opName
            override def apply(actual: Option[X], displayActualValue: Boolean): Validation[Option[X]] = validator(actual, session)
          }.success
        )

      override def is(expected: Expression[X])(implicit equality: Equality[X]): Final[T, P] =
        validate(expected.map(new Matcher.Is(_, equality)))
      override def isNull: Final[T, P] = validate(new Matcher.IsNull[X].expressionSuccess)
      override def not(expected: Expression[X])(implicit equality: Equality[X]): Final[T, P] =
        validate(expected.map(new Matcher.Not(_, equality)))
      override def notNull: Final[T, P] = validate(new Matcher.NotNull[X].expressionSuccess)
      override def in(expected: X*): Final[T, P] = validate(expected.toSeq.expressionSuccess.map(new Matcher.In(_)))
      override def in(expected: Expression[Seq[X]]): Final[T, P] = validate(expected.map(new Matcher.In(_)))
      override def exists: Final[T, P] = validate(new Validator.Exists[X]().expressionSuccess)
      override def notExists: Final[T, P] = validate(new Validator.NotExists[X]().expressionSuccess)
      override def optional: Final[T, P] = validate(new Validator.Optional[X]().expressionSuccess)
      override def lt(expected: Expression[X])(implicit ordering: Ordering[X]): Final[T, P] =
        validate(expected.map(new Matcher.Compare("lessThan", "less than", ordering.lt, _)))
      override def lte(expected: Expression[X])(implicit ordering: Ordering[X]): Final[T, P] =
        validate(expected.map(new Matcher.Compare("lessThanOrEqual", "less than or equal to", ordering.lteq, _)))
      override def gt(expected: Expression[X])(implicit ordering: Ordering[X]): Final[T, P] =
        validate(expected.map(new Matcher.Compare("greaterThan", "greater than", ordering.gt, _)))
      override def gte(expected: Expression[X])(implicit ordering: Ordering[X]): Final[T, P] =
        validate(expected.map(new Matcher.Compare("greaterThanOrEqual", "greater than or equal to", ordering.gteq, _)))
    }
  }

  trait Final[T, P] extends CheckBuilder[T, P] {
    def name(n: String): Final[T, P]
    def saveAs(key: String): Final[T, P]
  }

  object Final {
    final case class Default[T, P, X](
        extractor: Expression[Extractor[P, X]],
        validator: Expression[Validator[X]],
        displayActualValue: Boolean,
        customName: Option[String],
        saveAs: Option[String]
    ) extends Final[T, P] {
      override def name(n: String): Final[T, P] = copy(customName = Some(n))

      override def saveAs(key: String): Final[T, P] = copy(saveAs = Some(key))

      override def build[C <: Check[R], R](materializer: CheckMaterializer[T, C, R, P]): C =
        materializer.materialize(Check.Default(_, extractor, validator, displayActualValue, customName, None, saveAs))
    }
  }
}
