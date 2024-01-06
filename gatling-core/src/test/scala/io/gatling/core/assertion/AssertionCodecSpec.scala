/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.core.assertion

import io.gatling.BaseSpec
import io.gatling.commons.stats.assertion._

import boopickle.Default._
import org.scalacheck.{ Arbitrary, Gen }

trait AssertionGenerator {
  private val doubleGen = Arbitrary.arbitrary[Double]

  private val pathGen = {
    val detailsGen = for (parts <- Gen.nonEmptyListOf(Gen.alphaStr.suchThat(_.nonEmpty))) yield AssertionPath.Details(parts)
    Gen.frequency(33 -> Gen.const(AssertionPath.Global), 33 -> Gen.const(AssertionPath.ForAll), 33 -> detailsGen)
  }

  private val targetGen = {
    val countTargetGen = {
      val countMetricGen = Gen.oneOf(CountMetric.AllRequests, CountMetric.FailedRequests, CountMetric.SuccessfulRequests)
      val countSelectionGen = Gen.oneOf(Target.Count(_), Target.Percent(_))

      for {
        metric <- countMetricGen
        selection <- countSelectionGen
      } yield selection(metric)
    }

    val timeTargetGen = {
      val timeMetricGen = Gen.const(TimeMetric.ResponseTime)
      val percentiles = (0 until 100).map(Stat.Percentile(_))
      val statGen = Gen.oneOf(Seq(Stat.Min, Stat.Max, Stat.Mean, Stat.StandardDeviation) ++ percentiles)

      for {
        metric <- timeMetricGen
        stat <- statGen
      } yield Target.Time(metric, stat)
    }

    Gen.oneOf(countTargetGen, timeTargetGen, Gen.const(Target.MeanRequestsPerSecond))
  }

  private val conditionGen = {
    val lessThan = for (d <- doubleGen) yield Condition.Lt(d)
    val greaterThan = for (d <- doubleGen) yield Condition.Gt(d)
    val is = for (d <- doubleGen) yield Condition.Is(d)
    val between = for {
      d1 <- doubleGen
      d2 <- doubleGen
    } yield Condition.Between(d1, d2, inclusive = true)
    val in = for (doubleList <- Gen.nonEmptyListOf(doubleGen)) yield Condition.In(doubleList)

    Gen.oneOf(lessThan, greaterThan, is, between, in)
  }

  val assertionGen: Gen[Assertion] =
    for {
      path <- pathGen
      target <- targetGen
      condition <- conditionGen
    } yield Assertion(path, target, condition)
}

class AssertionCodecSpec extends BaseSpec with AssertionGenerator {
  override implicit val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(minSuccessful = 300)

  "The assertion parser" should "be able to parse correctly arbitrary assertions" in {
    forAll(assertionGen) { assertion =>
      val bytes = Pickle.intoBytes(assertion)
      val roundtrip = Unpickle[Assertion].fromBytes(bytes)

      roundtrip shouldBe assertion
    }
  }
}
