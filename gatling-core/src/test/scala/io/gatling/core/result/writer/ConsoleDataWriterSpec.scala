/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.result.writer

import scala.collection.mutable

import org.threeten.bp.LocalDateTime

import org.scalatest.{ FlatSpec, Matchers }

import io.gatling.core.config.GatlingConfiguration

class ConsoleDataWriterSpec extends FlatSpec with Matchers {

  GatlingConfiguration.setUpForTest()

  val time = LocalDateTime.of(2012, 8, 24, 13, 37)

  def lines(summary: ConsoleSummary) = summary.text.toString.split("\r?\n")

  def progressBar(summary: ConsoleSummary) = lines(summary)(4)

  def requestsInfo(summary: ConsoleSummary) = lines(summary).splitAt(6)._2.mkString(sys.props("line.separator"))

  "console summary progress bar" should "handle it correctly when all the users are waiting" in {

    val counters = new UserCounters(11)

    val summary = ConsoleSummary(10000, mutable.Map("request1" -> counters), new RequestCounters, mutable.Map.empty, mutable.Map.empty, time)
    summary.complete shouldBe false
    progressBar(summary) shouldBe "[                                                                          ]  0%"
  }

  it should "handle it correctly when all the users are running" in {

    val counters = new UserCounters(11)
    for (i <- 1 to 11) counters.userStart()

    val summary = ConsoleSummary(10000, mutable.Map("request1" -> counters), new RequestCounters, mutable.Map.empty, mutable.Map.empty, time)
    summary.complete shouldBe false
    progressBar(summary) shouldBe "[--------------------------------------------------------------------------]  0%"
  }

  it should "handle it correctly when all the users are done" in {

    val counters = new UserCounters(11)
    for (i <- 1 to 11) counters.userStart()
    for (i <- 1 to 11) counters.userDone()

    val summary = ConsoleSummary(10000, mutable.Map("request1" -> counters), new RequestCounters, mutable.Map.empty, mutable.Map.empty, time)
    summary.complete shouldBe true
    progressBar(summary) shouldBe "[##########################################################################]100%"
  }

  it should "handle it correctly when there are running and done users" in {

    val counters = new UserCounters(11)
    for (i <- 1 to 11) counters.userStart()
    for (i <- 1 to 10) counters.userDone()

    val summary = ConsoleSummary(10000, mutable.Map("request1" -> counters), new RequestCounters, mutable.Map.empty, mutable.Map.empty, time)
    summary.complete shouldBe false
    progressBar(summary) shouldBe "[###################################################################-------] 90%"
  }

  "console summary" should "display requests without errors" in {
    val requestCounters = mutable.Map("request1" -> new RequestCounters(20, 0))

    val summary = ConsoleSummary(10000, mutable.Map("request1" -> new UserCounters(11)), new RequestCounters(20, 0), requestCounters, mutable.Map.empty, time)

    val actual = requestsInfo(summary)
    actual shouldBe """---- Requests ------------------------------------------------------------------
                      |> Global                                                   (OK=20     KO=0     )
                      |> request1                                                 (OK=20     KO=0     )
                      |================================================================================""".stripMargin
  }

  it should "display requests with multiple errors" in {
    val requestCounters = mutable.Map("request1" -> new RequestCounters(0, 20))

    val errorsCounters1 = mutable.Map("error1" -> 19, "error2" -> 1)
    val summary1 = ConsoleSummary(10000, mutable.Map("request1" -> new UserCounters(11)), new RequestCounters(0, 20),
      requestCounters, errorsCounters1, time)

    val output = requestsInfo(summary1)

    output shouldBe s"""---- Requests ------------------------------------------------------------------
          |> Global                                                   (OK=0      KO=20    )
          |> request1                                                 (OK=0      KO=20    )
          |---- Errors --------------------------------------------------------------------
          |> error1                                                             19 (${ConsoleErrorsWriter.formatPercent(95.0)}%)
          |> error2                                                              1 ( ${ConsoleErrorsWriter.formatPercent(5.0)}%)
          |================================================================================""".stripMargin
    all(output.lines.map(_.length).toSet) shouldBe <=(80)
  }

  it should "display requests with high number of errors" in {
    val requestCounters = mutable.Map("request1" -> new RequestCounters(0, 123456))
    val loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
    val errorsCounters = mutable.Map(loremIpsum -> 123456)
    val summary = ConsoleSummary(10000, mutable.Map("request1" -> new UserCounters(11)), new RequestCounters(0, 123456),
      requestCounters, errorsCounters, time)

    val output = requestsInfo(summary)

    output shouldBe s"""---- Requests ------------------------------------------------------------------
          |> Global                                                   (OK=0      KO=123456)
          |> request1                                                 (OK=0      KO=123456)
          |---- Errors --------------------------------------------------------------------
          |> Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed  123456 (${ConsoleErrorsWriter.OneHundredPercent}%)
          |do eiusmod tempor incididunt ut labore et dolore magna aliqua....
          |================================================================================""".stripMargin
    all(output.lines.map(_.length).toSet) shouldBe <=(80)
  }
}
