/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.feeder

import com.excilys.ebi.gatling.core.log.Logging

/**
 * Abstract class for all Feeders of the application
 *
 * @param filePath file path of the seed file
 * @param mappings the mappings for the feeder, ie the keys of the values in the context
 */
abstract class Feeder(val filePath: String, val mappings: List[String]) extends Logging {
	/**
	 * Gets the next line of the feeder
	 *
	 * @return a map containing the values of the line with the feeder keys as keys
	 */
	def next: Map[String, String]
}