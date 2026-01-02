/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util.DefaultClock
import io.gatling.core.EmptySession
import io.gatling.core.actor.ActorSpec
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine

class FeedSpec extends ActorSpec with EmptySession {
  private val clock = new DefaultClock

  "Feed" should "send a FeedMessage to the SingletonFeed actor" in {
    val feedActor = mockActorRef[FeedMessage]("feed")
    val nextActor = mockActorRef[Session]("next")
    val next = new ActorDelegatingAction("next", nextActor)

    val feed = new Feed(feedActor, None, null, clock, next)

    feed ! emptySession

    val feedMessage = feedActor.expectMsgType[FeedMessage]()
    feedMessage.session shouldBe emptySession
    feedMessage.next shouldBe next
  }
}
