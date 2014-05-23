/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.util

import scala.concurrent.duration._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification

import TimeHelper._

@RunWith(classOf[JUnitRunner])
class TimeHelperSpec extends Specification {
  "TimeHelper" should {
    "toMillisPrecision should work correctly" in {
      val t1 = 100.nanoseconds
      toMillisPrecision(t1).toNanos should beEqualTo(0)

      val t2 = 1234.microseconds
      toMillisPrecision(t2).toMicros should beEqualTo(1000)

      val t3 = 1.minute
      toMillisPrecision(t3).toMicros should beEqualTo(1 * 60 * 1000 * 1000)
    }
  }

  // Deactivate Specs2 implicit to be able to use the ones provided in scala.concurrent.duration
  override def intToRichLong(v: Int) = super.intToRichLong(v)
}

