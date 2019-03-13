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
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine

import akka.testkit._
import org.scalatestplus.mockito.MockitoSugar

class FeedSpec extends AkkaSpec with MockitoSugar {

  private val clock = new DefaultClock

  "Feed" should "send a FeedMessage to the SingletonFeed actor" in {
    val singleton = TestProbe()
    val next = new ActorDelegatingAction("next", self)

    val feed = new Feed(singleton.ref, 1.expressionSuccess, mock[StatsEngine], clock, next)

    val session = Session("scenario", 0, clock.nowMillis)

    feed ! session

    val feedMessage = singleton.expectMsgType[FeedMessage]
    feedMessage.session shouldBe session
    feedMessage.next shouldBe next
  }
}
