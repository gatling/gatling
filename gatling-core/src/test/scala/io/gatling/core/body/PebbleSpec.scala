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

package io.gatling.core.body

import java.{ util => ju }

import scala.collection.immutable
import scala.collection.mutable

import io.gatling.BaseSpec
import io.gatling.core.session.SessionPrivateAttributes

class PebbleSpec extends BaseSpec {

  "sessionAttributesToJava" should "convert attributes of type immutable.Map" in {
    val input = Map("foo" -> immutable.Map("bar" -> "baz"))
    val output = Pebble.sessionAttributesToJava(input)
    output.size shouldBe 1
    output.get("foo").asInstanceOf[ju.Map[_, _]].get("bar") shouldBe "baz"
  }

  it should "convert attributes of type mutable.Map" in {
    val input = Map("foo" -> mutable.Map("bar" -> "baz"))
    val output = Pebble.sessionAttributesToJava(input)
    output.size shouldBe 1
    output.get("foo").asInstanceOf[ju.Map[_, _]].get("bar") shouldBe "baz"
  }

  it should "convert attributes of type immutable.Seq" in {
    val input = Map("foo" -> immutable.Seq("bar"))
    val output = Pebble.sessionAttributesToJava(input)
    output.size shouldBe 1
    output.get("foo").asInstanceOf[ju.List[_]].get(0) shouldBe "bar"
  }

  it should "convert attributes of type mutable.Seq" in {
    val input = Map("foo" -> mutable.Seq("bar"))
    val output = Pebble.sessionAttributesToJava(input)
    output.size shouldBe 1
    output.get("foo").asInstanceOf[ju.List[_]].get(0) shouldBe "bar"
  }

  it should "convert attributes of type immutable.Set" in {
    val input = Map("foo" -> immutable.Set("bar"))
    val output = Pebble.sessionAttributesToJava(input)
    output.size shouldBe 1
    output.get("foo").asInstanceOf[ju.Set[_]].iterator.next() shouldBe "bar"
  }

  it should "convert attributes of type mutable.Set" in {
    val input = Map("foo" -> mutable.Set("bar"))
    val output = Pebble.sessionAttributesToJava(input)
    output.size shouldBe 1
    output.get("foo").asInstanceOf[ju.Set[_]].iterator.next() shouldBe "bar"
  }

  it should "not convert other types" in {
    val input = Map("foo" -> 1, "bar" -> "baz")
    val output = Pebble.sessionAttributesToJava(input)
    output.size shouldBe 2
    output.get("foo") shouldBe 1
    output.get("bar") shouldBe "baz"
  }

  it should "remove any Gatling private attribute" in {
    val input = Map("foo" -> "bar", SessionPrivateAttributes.PrivateAttributePrefix + "hello" -> "world")
    val output = Pebble.sessionAttributesToJava(input)
    output.size shouldBe 1
  }
}
