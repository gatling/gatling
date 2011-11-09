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
package com.excilys.ebi.gatling.core.util

import com.excilys.ebi.gatling.core.util.StringHelper._

/**
 * This object groups all utilities for properties
 */
object PropertiesHelper {
	/**
	 * Property used to specify the location of Gatling configuration file
	 */
	val GATLING_CONFIG_PROPERTY = Option(System.getProperty("gatling.config")) map (_.trim()) getOrElse EMPTY

	/**
	 * Property used to specify whether or not to generate statistics after the simulation
	 */
	val NO_STATS_PROPERTY = Option(System.getProperty("NoStats")) map (_.toBoolean) getOrElse false

	/**
	 * Property used to specify whether or not to generate statistics only from a previous simulation
	 */
	val ONLY_STATS_PROPERTY = Option(System.getProperty("OnlyStats")) map (_.toBoolean) getOrElse false
}