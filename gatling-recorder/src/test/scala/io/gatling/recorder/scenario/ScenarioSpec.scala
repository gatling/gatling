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
package io.gatling.recorder.scenario

import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class ScenarioSpec extends Specification {

	"groupAsLongAsPredicate()" should {

		"group elements as long as the predicate applies" in {
			val reqs = Vector(200, 200, 304, 304, 200, 304, 200, 200).zipWithIndex.map(_.swap)
			val expectedReqs = List(
				((0, 200) :: Nil),
				((1, 200) :: Nil),
				((2, 304) :: (3, 304) :: (4, 200) :: Nil),
				((5, 304) :: (6, 200) :: Nil),
				((7, 200) :: Nil))

			val groupReqs = Scenario.groupAsLongAsPredicate((t: (Int, Int)) => t._2 == 304)(reqs)

			groupReqs must beEqualTo(expectedReqs)
		}
	}
}
