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

import com.typesafe.config.ConfigFactory

object AkkaConfiguration {

	def setUp = {
		val classLoader = getClass.getClassLoader
		val defaultsConfig = ConfigFactory.parseResources(classLoader,"akka-defaults.conf")
		val customConfig = ConfigFactory.parseResources(classLoader,"akka.conf")
		ConfigFactory.systemProperties.withFallback(customConfig).withFallback(defaultsConfig)
	}
}
