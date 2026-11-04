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

package io.gatling.http.action

import scala.concurrent.duration._

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class SetCheckBufferSpec extends AnyFlatSpecLike with Matchers {
  private final case class Message(timestamp: Long, value: String)

  private def newBuffer(maxSize: Int, maxAge: FiniteDuration) = new SetCheckBuffer[Message](maxSize, maxAge, _.timestamp)

  "SetCheckBuffer" should "evict the oldest messages when full" in {
    val buffer = newBuffer(2, 1.minute)
    buffer.add(Message(1, "a"), 1)
    buffer.add(Message(2, "b"), 2)
    buffer.add(Message(3, "c"), 3)
    buffer.removeAll(3).map(_.value) shouldBe List("b", "c")
    buffer.removeAll(3) shouldBe empty
  }

  it should "discard expired messages" in {
    val buffer = newBuffer(10, 100.millis)
    buffer.add(Message(0, "a"), 0)
    buffer.add(Message(50, "b"), 50)
    buffer.removeAll(120).map(_.value) shouldBe List("b")
  }

  it should "put restored messages before the ones added since" in {
    val buffer = newBuffer(3, 1.minute)
    buffer.add(Message(1, "a"), 1)
    buffer.add(Message(2, "b"), 2)
    val removed = buffer.removeAll(2)
    buffer.add(Message(3, "c"), 3)
    buffer.restore(removed)
    buffer.removeAll(3).map(_.value) shouldBe List("a", "b", "c")
  }

  it should "not buffer anything when disabled" in {
    val noSize = newBuffer(0, 1.minute)
    noSize.add(Message(1, "a"), 1)
    noSize.removeAll(1) shouldBe empty

    val noAge = newBuffer(10, Duration.Zero)
    noAge.add(Message(1, "a"), 1)
    noAge.removeAll(1) shouldBe empty
  }
}
