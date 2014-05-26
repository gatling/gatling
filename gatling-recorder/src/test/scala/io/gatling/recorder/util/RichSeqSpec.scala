/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.recorder.util

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import io.gatling.recorder.util.collection._

@RunWith(classOf[JUnitRunner])
class RichSeqSpec extends Specification {

  "groupAsLongAs()" should {
    "group elements as long as the predicate applies" in {
      val reqs = Vector(200, 200, 304, 304, 200, 304, 200, 200).zipWithIndex.map(_.swap)
      val expectedReqs = List(
        List(0 -> 200),
        List(1 -> 200),
        List(2 -> 304, 3 -> 304, 4 -> 200),
        List(5 -> 304, 6 -> 200),
        List(7 -> 200))

      val groupedReqs = reqs.groupAsLongAs((t: (Int, Int)) => t._2 == 304)

      groupedReqs must beEqualTo(expectedReqs)
    }
  }

  "splitWhen()" should {
    "split the current sequence everytime the predicate applies" in {
      val xs = List(1, 2, 2, 3, 3, 4, 4, 5)
      val rs = xs.splitWhen((x: Int) => x % 2 == 0)
      rs must beEqualTo(List(List(1), List(2), List(2, 3, 3), List(4), List(4, 5)))
    }
  }

}
