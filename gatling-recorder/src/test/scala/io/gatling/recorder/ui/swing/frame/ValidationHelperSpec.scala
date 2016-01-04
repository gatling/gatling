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
package io.gatling.recorder.ui.swing.frame

import io.gatling.BaseSpec
import io.gatling.recorder.ui.swing.frame.ValidationHelper._

class ValidationHelperSpec extends BaseSpec {

  "isValidPackageName" should "accept lower case" in {
    isValidPackageName("foo.bar") shouldBe true
  }

  it should "reject upper case" in {
    isValidPackageName("foo.Bar") shouldBe false
  }

  it should "accept digits" in {
    isValidPackageName("foo2.bar") shouldBe true
  }

  it should "reject digits when first directory char" in {
    isValidPackageName("foo.2bar") shouldBe false
  }

  it should "accept underscore" in {
    isValidPackageName("fo_o.bar") shouldBe true
  }

  it should "reject whitespaces" in {
    isValidPackageName("foo .bar") shouldBe false
  }

  "isValidSimpleClassName" should "accept lower case and upper case" in {
    isValidSimpleClassName("FooBar") shouldBe true
  }

  it should "reject digit first char" in {
    isValidSimpleClassName("1FooBar") shouldBe false
  }

  it should "accept digit char in other positions" in {
    isValidSimpleClassName("Foo1Bar") shouldBe true
  }

  it should "reject whitespaces" in {
    isValidSimpleClassName("Foo Bar") shouldBe false
  }

  it should "reject underscore" in {
    isValidSimpleClassName("Foo_Bar") shouldBe false
  }
}
