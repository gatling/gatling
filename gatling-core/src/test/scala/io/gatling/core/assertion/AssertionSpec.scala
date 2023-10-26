/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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
import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.shared.model.assertion.{ Assertion, AssertionStatsRepository, AssertionValidator }

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class Stats(
    stats: AssertionStatsRepository.Stats,
    group: List[String] = Nil,
    request: Option[String] = None,
    status: Option[Status] = None
)

class AssertionSpec extends BaseSpec with AssertionSupport {
  implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private type Conditions[T] = List[AssertionWithPathAndTarget[T] => Assertion]
  private type StatsModifiers = List[Stats => Stats]

  private val SetRequestThenGroupModifiers: StatsModifiers =
    List(_.copy(request = Some("foo")), _.copy(group = "foo" :: Nil))

  private def statsRepository[T: Numeric](stats: Stats*): AssertionStatsRepository =
    new AssertionStatsRepository() {

      override def allRequestPaths(): List[AssertionStatsRepository.StatsPath.Request] =
        stats.collect { case Stats(_, group, Some(request), _) =>
          AssertionStatsRepository.StatsPath.Request(group, request)
        }.toList

      override def findPathByParts(parts: List[String]): Option[AssertionStatsRepository.StatsPath] =
        stats.collectFirst {
          case Stats(_, group, Some(request), _) if group ::: request :: Nil == parts => AssertionStatsRepository.StatsPath.Request(group, request)
          case Stats(_, group, None, _) if group == parts                             => AssertionStatsRepository.StatsPath.Group(group)
        }

      override def requestGeneralStats(group: List[String], request: Option[String], status: Option[Status]): AssertionStatsRepository.Stats =
        stats
          .collectFirst { case Stats(stats, `group`, `request`, `status`) => stats }
          .getOrElse(AssertionStatsRepository.Stats.NoData)

      override def groupCumulatedResponseTimeGeneralStats(group: List[String], status: Option[Status]): AssertionStatsRepository.Stats =
        stats
          .collectFirst { case Stats(stats, `group`, None, `status`) => stats }
          .getOrElse(AssertionStatsRepository.Stats.NoData)
    }

  private def validateAssertions(repository: AssertionStatsRepository, assertions: Assertion*): Boolean =
    new AssertionValidator(repository).validateAssertions(assertions.toList).forall(_.success)

  "Assertion" should "fail the assertion when the request path does not exist" in {
    val requestStats = Stats(AssertionStatsRepository.Stats.NoData, request = Some("bar"))
    val repository1 = statsRepository[Double](requestStats)
    validateAssertions(repository1, details("foo").requestsPerSec.is(100)) shouldBe false

    val groupStats = Stats(AssertionStatsRepository.Stats.NoData, group = List("bar"))
    val repository2 = statsRepository[Double](groupStats)
    validateAssertions(repository2, details("foo").requestsPerSec.is(100)) shouldBe false

    val requestAndGroupStats = Stats(AssertionStatsRepository.Stats.NoData, request = Some("baz"), group = List("bar"))
    val repository3 = statsRepository[Double](requestAndGroupStats)
    validateAssertions(repository3, details("baz").requestsPerSec.is(100)) shouldBe false
  }

  // TODO : add test on global and forAll
  it should "be able to validate a meanRequestsPerSec assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestAndGroupStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(meanRequestsPerSec = 5)))
      val conditions: Conditions[Double] = List(_.lte(10), _.gte(3), _.is(5), _.between(4, 6), _.in(1, 3, 5, 7))
      val assertions = conditions.map(_.apply(details("foo").requestsPerSec))
      val repository = statsRepository[Double](requestAndGroupStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a successfulRequests.count assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(count = 5), status = Some(OK)))
      val conditions: Conditions[Long] = List(_.lte(10), _.gte(3), _.is(5), _.between(4, 6), _.in(1, 3, 5, 7))
      val assertions = conditions.map(_.apply(details("foo").successfulRequests.count))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a failedRequests.count assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(count = 5), status = Some(KO)))
      val conditions: Conditions[Long] = List(_.lte(10), _.gte(3), _.is(5), _.between(4, 6), _.in(1, 3, 5, 7))
      val assertions = conditions.map(_.apply(details("foo").failedRequests.count))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a allRequests.count assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(count = 10)))
      val conditions: Conditions[Long] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val assertions = conditions.map(_.apply(details("foo").allRequests.count))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a successfulRequests.percent assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val successful = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(count = 10)))
      val failed = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(count = 5), status = Some(OK)))
      val conditions: Conditions[Double] = List(_.lte(60), _.gte(30), _.is(50), _.between(40, 60), _.in(20, 40, 50, 80))
      val assertions = conditions.map(_.apply(details("foo").successfulRequests.percent))
      val repository = statsRepository[Long](successful, failed)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a failedRequests.percent assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val failed = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(count = 10)))
      val successful = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(count = 5), status = Some(KO)))
      val conditions: Conditions[Double] = List(_.lte(60), _.gte(30), _.is(50), _.between(40, 60), _.in(20, 40, 50, 80))
      val assertions = conditions.map(_.apply(details("foo").failedRequests.percent))
      val repository = statsRepository[Long](failed, successful)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a allRequests.percent assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(count = 10)))
      val conditions: Conditions[Double] = List(_.lte(110), _.gte(90), _.is(100), _.between(80, 120), _.in(90, 100, 130))
      val assertions = conditions.map(_.apply(details("foo").allRequests.percent))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a responseTime.min assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(min = 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val assertions = conditions.map(_.apply(details("foo").responseTime.min))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a responseTime.max assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(max = 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val assertions = conditions.map(_.apply(details("foo").responseTime.max))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a responseTime.mean assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(mean = 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val assertions = conditions.map(_.apply(details("foo").responseTime.mean))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a responseTime.stdDev assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(stdDev = 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val assertions = conditions.map(_.apply(details("foo").responseTime.stdDev))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a responseTime.percentiles1 assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(percentile = _ => 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val assertions = conditions.map(_.apply(details("foo").responseTime.percentile1))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a responseTime.percentiles2 assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(percentile = _ => 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val assertions = conditions.map(_.apply(details("foo").responseTime.percentile2))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a responseTime.percentiles3 assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(percentile = _ => 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val assertions = conditions.map(_.apply(details("foo").responseTime.percentile3))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }

  // TODO : add test on global and forAll
  it should "be able to validate a responseTime.percentiles4 assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(AssertionStatsRepository.Stats.NoData.copy(percentile = _ => 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val assertions = conditions.map(_.apply(details("foo").responseTime.percentile4))
      val repository = statsRepository[Long](requestStats)
      validateAssertions(repository, assertions: _*) shouldBe true
    }
  }
}
