/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.util
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import com.excilys.ebi.gatling.core.session.Session

@RunWith(classOf[JUnitRunner])
class StringHelperSpec extends Specification {

	"""foo${bar}""" should {

		"""produce fooBAR with Map("bar" -> "BAR")""" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			StringHelper.parseEvaluatable("foo${bar}")(session) must beEqualTo("fooBAR")
		}
	}

	"""${bar}baz""" should {

		"""produce BARbaz with Map("bar" -> "BAR")""" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			StringHelper.parseEvaluatable("${bar}baz")(session) must beEqualTo("BARbaz")
		}
	}

	"""${bar}baz""" should {

		"""produce BARbaz with Map("bar" -> "BAR")""" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			StringHelper.parseEvaluatable("${bar}baz")(session) must beEqualTo("BARbaz")
		}
	}

	"""${foo} ${bar}""" should {

		"""produce FOO BAR with Map("foo" -> "FOO", "bar" -> "BAR")""" in {
			val session = new Session("scenario", 1, Map("foo" -> "FOO", "bar" -> "BAR"))
			StringHelper.parseEvaluatable("${foo} ${bar}")(session) must beEqualTo("FOO BAR")
		}
	}

	"""foo${bar}baz""" should {

		"""produce fooBARbaz with Map("bar" -> "BAR")""" in {
			val session = new Session("scenario", 1, Map("bar" -> "BAR"))
			StringHelper.parseEvaluatable("foo${bar}baz")(session) must beEqualTo("fooBARbaz")
		}
	}

	"""foo${bar(1)}""" should {

		"""produce fooBAR2 with Map("bar" -> List("BAR1", "BAR2"))""" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2")))
			StringHelper.parseEvaluatable("foo${bar(1)}")(session) must beEqualTo("fooBAR2")
		}
	}

	"""{foo${bar(1)}}""" should {

		"""produce {fooBAR2} with Map("bar" -> List("BAR1", "BAR2"))""" in {
			val session = new Session("scenario", 1, Map("bar" -> List("BAR1", "BAR2")))
			StringHelper.parseEvaluatable("{foo${bar(1)}}")(session) must beEqualTo("{fooBAR2}")
		}
	}
}