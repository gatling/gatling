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
package io.gatling.core.config

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProtocolSpec extends Specification {

	class FooProtocol(val foo: String) extends Protocol
	class BarProtocol(val bar: String) extends Protocol

	"building registry" should {

		"return the configuration when 1 configuration" in {
			Protocols().register(new FooProtocol("foo")).getProtocol[FooProtocol] must beSome.which(_.foo == "foo")
		}

		"return the configurations when 2 different configurations" in {
			val registry = Protocols().register(Seq(new FooProtocol("foo"), new BarProtocol("bar")))
			registry.getProtocol[FooProtocol] must beSome.which(_.foo == "foo")
			registry.getProtocol[BarProtocol] must beSome.which(_.bar == "bar")
		}

		"not fail when no configuration" in {
			Protocols().getProtocol[FooProtocol] must beNone
		}

		"override with latest when multiple configurations of the same type" in {
			Protocols().register(Seq(new FooProtocol("foo1"), new FooProtocol("foo2"))).getProtocol[FooProtocol] must beSome.which(_.foo == "foo2")
		}
	}
}
