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

package io.gatling.core.config

import io.gatling.BaseSpec
import io.gatling.core.protocol.{ Protocols, Protocol }
import org.scalatest.OptionValues

final case class FooProtocol(foo: String) extends Protocol

final case class BarProtocol(bar: String) extends Protocol

class ProtocolSpec extends BaseSpec with OptionValues {

  "building registry" should "return the configuration when 1 configuration" in {
    Protocols(FooProtocol("foo")).protocol[FooProtocol].map(_.foo) shouldBe Some("foo")
  }

  it should "return the configurations when 2 different configurations" in {
    val protocols = Protocols(FooProtocol("foo"), BarProtocol("bar"))
    protocols.protocol[FooProtocol].map(_.foo) shouldBe Some("foo")
    protocols.protocol[BarProtocol].map(_.bar) shouldBe Some("bar")
  }

  it should "not fail when no configuration" in {
    Protocols().protocol[FooProtocol] shouldBe None
  }

  it should "override with latest when multiple configurations of the same type" in {
    Protocols(FooProtocol("foo1"), FooProtocol("foo2")).protocol[FooProtocol].map(_.foo) shouldBe Some("foo2")
  }
}
