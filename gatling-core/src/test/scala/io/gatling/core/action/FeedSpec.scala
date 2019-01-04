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

package io.gatling.core.action

import io.gatling.AkkaSpec
import io.gatling.commons.util.DefaultClock
import io.gatling.commons.validation._
import io.gatling.core.session._
import io.gatling.core.stats.DataWritersStatsEngine

import akka.testkit._

class FeedSpec extends AkkaSpec {

  private val clock = new DefaultClock

  "Feed" should "send a FeedMessage to the SingletonFeed actor" in {
    val dataWriterProbe = TestProbe()
    val statsEngine = new DataWritersStatsEngine(List(dataWriterProbe.ref), system, clock)
    val singleton = TestProbe()
    val controller = TestProbe()
    val number: Expression[Int] = session => 1.success
    val next = new ActorDelegatingAction("next", self)

    val feed = new Feed(singleton.ref, number, controller.ref, statsEngine, clock, next)

    val session = Session("scenario", 0, clock.nowMillis)

    feed ! session

    val feedMessage = singleton.expectMsgType[FeedMessage]
    feedMessage.session shouldBe session
    feedMessage.controller shouldBe controller.ref
    feedMessage.next shouldBe next
  }
}
