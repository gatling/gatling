/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.config

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProtocolConfigurationRegistrySpec extends Specification {
	
	class FooProtocolConfiguration(val foo: String) extends ProtocolConfiguration
	class BarProtocolConfiguration(val bar: String) extends ProtocolConfiguration

	"building registry" should {

		"return the configuration when 1 configuration" in {
			ProtocolConfigurationRegistry(List(new FooProtocolConfiguration("foo"))).getProtocolConfiguration[FooProtocolConfiguration] must beSome.which(_.foo == "foo")
		}

		"return the configurations when 2 different configurations" in {
			val registry = ProtocolConfigurationRegistry(List(new FooProtocolConfiguration("foo"), new BarProtocolConfiguration("bar")))
			registry.getProtocolConfiguration[FooProtocolConfiguration] must beSome.which(_.foo == "foo")
			registry.getProtocolConfiguration[BarProtocolConfiguration] must beSome.which(_.bar == "bar")
		}

		"not fail when no configuration" in {
			ProtocolConfigurationRegistry(List.empty).getProtocolConfiguration[FooProtocolConfiguration] must beNone
		}

		"fail when multiple configurations of the same type" in {
			ProtocolConfigurationRegistry(List(new FooProtocolConfiguration("foo1"), new FooProtocolConfiguration("foo2"))) must throwA[ExceptionInInitializerError]
		}
	}
}