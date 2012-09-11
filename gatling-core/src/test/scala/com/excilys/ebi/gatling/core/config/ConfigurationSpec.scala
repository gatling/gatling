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
package com.excilys.ebi.gatling.core.config

import java.util.{ HashMap => JHashMap, Map => JMap }

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConfigurationSpec extends Specification {

	"building from PropertiesBuilder should support List property" in {
		
		val props = new GatlingPropertiesBuilder
		props.clazz("Foo")
		
		val map = props.build
		
		GatlingConfiguration.setUp(props.build)
		
		GatlingConfiguration.configuration.simulation.clazz should beEqualTo("Foo")
	}
}