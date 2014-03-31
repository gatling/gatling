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
package io.gatling.recorder.ui.swing.frame

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import ValidationHelper._

@RunWith(classOf[JUnitRunner])
class ValidationHelperSpec extends Specification {

  "isValidPackageName" should {

    "accept lower case" in {
      isValidPackageName("foo.bar") should beTrue
    }

    "reject upper case" in {
      isValidPackageName("foo.Bar") should beFalse
    }

    "accept digits" in {
      isValidPackageName("foo2.bar") should beTrue
    }

    "reject digits when first directory char" in {
      isValidPackageName("foo.2bar") should beFalse
    }

    "accept underscore" in {
      isValidPackageName("fo_o.bar") should beTrue
    }

    "reject whitespaces" in {
      isValidPackageName("foo .bar") should beFalse
    }
  }

  "isValidSimpleClassName" should {

    "accept lower case and upper case" in {
      isValidSimpleClassName("FooBar") should beTrue
    }

    "reject digit first char" in {
      isValidSimpleClassName("1FooBar") should beFalse
    }

    "accept digit char in other positions" in {
      isValidSimpleClassName("Foo1Bar") should beTrue
    }

    "reject whitespaces" in {
      isValidSimpleClassName("Foo Bar") should beFalse
    }

    "reject underscore" in {
      isValidSimpleClassName("Foo_Bar") should beFalse
    }
  }
}
