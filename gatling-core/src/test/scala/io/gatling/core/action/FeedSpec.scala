/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
import io.gatling.commons.validation._
import io.gatling.core.session._
import io.gatling.core.stats.DataWritersStatsEngine

import akka.testkit._

class FeedSpec extends AkkaSpec {

  "Feed" should "send a FeedMessage to the SingletonFeed actor" in {
    val dataWriterProbe = TestProbe()
    val statsEngine = new DataWritersStatsEngine(system, List(dataWriterProbe.ref))
    val singleton = TestProbe()
    val controller = TestProbe()
    val number: Expression[Int] = session => 1.success
    val next = new ActorDelegatingAction("next", self)

    val feed = new Feed(singleton.ref, number, controller.ref, statsEngine, next)

    val session = Session("scenario", 0)

    feed ! session

    val feedMessage = singleton.expectMsgType[FeedMessage]
    feedMessage.session shouldBe session
    feedMessage.controller shouldBe controller.ref
    feedMessage.next shouldBe next
  }
}
