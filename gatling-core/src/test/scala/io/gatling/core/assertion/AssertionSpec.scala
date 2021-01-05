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

package io.gatling.core.assertion

import io.gatling.BaseSpec
import io.gatling.commons.shared.unstable.model.stats.{ GeneralStats, GeneralStatsSource, Group, GroupStatsPath, RequestStatsPath, StatsPath }
import io.gatling.commons.shared.unstable.model.stats.assertion.AssertionValidator
import io.gatling.commons.stats._
import io.gatling.commons.stats.assertion.Assertion
import io.gatling.commons.util.StringHelper._
import io.gatling.core.config.GatlingConfiguration

import org.mockito.Mockito.when

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class Stats(
    generalStats: GeneralStats,
    requestName: String = "",
    groupPath: List[String] = Nil,
    status: Option[Status] = None
) {

  def request: Option[String] = requestName.trimToOption
  def group: Option[Group] = if (groupPath.nonEmpty) Some(Group(groupPath)) else None
}

class AssertionValidatorSpec extends BaseSpec with AssertionSupport {

  implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private type Conditions[T] = List[AssertionWithPathAndTarget[T] => Assertion]
  private type StatsModifiers = List[Stats => Stats]

  private val SetRequestThenGroupModifiers: StatsModifiers =
    List(_.copy(requestName = "foo"), _.copy(groupPath = List("foo")))

  private def generalStatsSource[T: Numeric](
      metric: AssertionWithPathAndTarget[T],
      conditions: Conditions[T],
      stats: Stats*
  ): GeneralStatsSource = {
    def mockAssertion(source: GeneralStatsSource): Unit =
      when(source.assertions) thenReturn conditions.map(_(metric))

    def mockStats(stat: Stats, source: GeneralStatsSource): Unit = {
      when(source.requestGeneralStats(stat.request, stat.group, stat.status)) thenReturn stat.generalStats
      stat.group.foreach { group =>
        when(source.groupCumulatedResponseTimeGeneralStats(group, stat.status)) thenReturn stat.generalStats
      }
    }

    def statsPaths: List[StatsPath] =
      stats
        .map(stat => (stat.request, stat.group))
        .map {
          case (Some(request), group) => RequestStatsPath(request, group)
          case (None, Some(group))    => GroupStatsPath(group)
          case _                      => throw new AssertionError("Can't have neither a request or group stats path")
        }
        .toList

    def mockStatsPath(source: GeneralStatsSource) =
      when(source.statsPaths) thenReturn statsPaths

    val mockedGeneralStatsSource = mock[GeneralStatsSource]

    mockAssertion(mockedGeneralStatsSource)
    stats.foreach(mockStats(_, mockedGeneralStatsSource))
    mockStatsPath(mockedGeneralStatsSource)

    mockedGeneralStatsSource
  }

  private def validateAssertions(source: GeneralStatsSource) =
    AssertionValidator.validateAssertions(source).map(_.result).forall(identity)

  "AssertionValidator" should "fail the assertion when the request path does not exist" in {
    val requestStats = Stats(GeneralStats.NoPlot, requestName = "bar")
    val source1 = generalStatsSource[Double](details("foo").requestsPerSec, List(_.is(100)), requestStats)
    validateAssertions(source1) shouldBe false

    val groupStats = Stats(GeneralStats.NoPlot, groupPath = List("bar"))
    val source2 = generalStatsSource[Double](details("foo").requestsPerSec, List(_.is(100)), groupStats)
    validateAssertions(source2) shouldBe false

    val requestAndGroupStats = Stats(GeneralStats.NoPlot, requestName = "baz", groupPath = List("bar"))
    val source3 = generalStatsSource[Double](details("baz").requestsPerSec, List(_.is(100)), requestAndGroupStats)
    validateAssertions(source3) shouldBe false
  }

  //TODO : add test on global and forAll
  it should "be able to validate a meanRequestsPerSec assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestAndGroupStats = modifier(Stats(GeneralStats.NoPlot.copy(meanRequestsPerSec = 5)))
      val conditions: Conditions[Double] = List(_.lte(10), _.gte(3), _.is(5), _.between(4, 6), _.in(1, 3, 5, 7))
      val source3 = generalStatsSource(details("foo").requestsPerSec, conditions, requestAndGroupStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a successfulRequests.count assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(count = 5), status = Some(OK)))
      val conditions: Conditions[Long] = List(_.lte(10), _.gte(3), _.is(5), _.between(4, 6), _.in(1, 3, 5, 7))
      val source3 = generalStatsSource(details("foo").successfulRequests.count, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a failedRequests.count assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(count = 5), status = Some(KO)))
      val conditions: Conditions[Long] = List(_.lte(10), _.gte(3), _.is(5), _.between(4, 6), _.in(1, 3, 5, 7))
      val source3 = generalStatsSource(details("foo").failedRequests.count, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a allRequests.count assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(count = 10)))
      val conditions: Conditions[Long] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val source3 = generalStatsSource(details("foo").allRequests.count, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a successfulRequests.percent assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val successful = modifier(Stats(GeneralStats.NoPlot.copy(count = 10)))
      val failed = modifier(Stats(GeneralStats.NoPlot.copy(count = 5), status = Some(OK)))
      val conditions: Conditions[Double] = List(_.lte(60), _.gte(30), _.is(50), _.between(40, 60), _.in(20, 40, 50, 80))
      val source3 = generalStatsSource(details("foo").successfulRequests.percent, conditions, successful, failed)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a failedRequests.percent assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val failed = modifier(Stats(GeneralStats.NoPlot.copy(count = 10)))
      val successful = modifier(Stats(GeneralStats.NoPlot.copy(count = 5), status = Some(KO)))
      val conditions: Conditions[Double] = List(_.lte(60), _.gte(30), _.is(50), _.between(40, 60), _.in(20, 40, 50, 80))
      val source3 = generalStatsSource(details("foo").failedRequests.percent, conditions, failed, successful)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a allRequests.percent assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(count = 10)))
      val conditions: Conditions[Double] = List(_.lte(110), _.gte(90), _.is(100), _.between(80, 120), _.in(90, 100, 130))
      val source3 = generalStatsSource(details("foo").allRequests.percent, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a responseTime.min assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(min = 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val source3 = generalStatsSource(details("foo").responseTime.min, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a responseTime.max assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(max = 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val source3 = generalStatsSource(details("foo").responseTime.max, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a responseTime.mean assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(mean = 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val source3 = generalStatsSource(details("foo").responseTime.mean, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a responseTime.stdDev assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(stdDev = 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val source3 = generalStatsSource(details("foo").responseTime.stdDev, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a responseTime.percentiles1 assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(percentile = _ => 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val source3 = generalStatsSource(details("foo").responseTime.percentile1, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a responseTime.percentiles2 assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(percentile = _ => 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val source3 = generalStatsSource(details("foo").responseTime.percentile2, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a responseTime.percentiles3 assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(percentile = _ => 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val source3 = generalStatsSource(details("foo").responseTime.percentile3, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }

  //TODO : add test on global and forAll
  it should "be able to validate a responseTime.percentiles4 assertion for requests and groups" in {
    for (modifier <- SetRequestThenGroupModifiers) {
      val requestStats = modifier(Stats(GeneralStats.NoPlot.copy(percentile = _ => 10)))
      val conditions: Conditions[Int] = List(_.lte(15), _.gte(8), _.is(10), _.between(8, 12), _.in(1, 3, 10, 13))
      val source3 = generalStatsSource(details("foo").responseTime.percentile4, conditions, requestStats)
      validateAssertions(source3) shouldBe true
    }
  }
}
