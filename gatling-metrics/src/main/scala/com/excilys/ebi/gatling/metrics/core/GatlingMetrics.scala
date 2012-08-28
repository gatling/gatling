/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.metrics.core

import com.excilys.ebi.gatling.metrics.types.{FastHistogram, FastCounter}


object GatlingMetrics {

	val registry = new GatlingMetricsRegistry

	def newFastCounter(klass: Class[_], name: String, scope: String = null): FastCounter =
		registry.newFastCounter(klass, name, scope)

	def newCachedFastCounter(klass: Class[_], name: String, scope: String = null): FastCounter =
		registry.newCachedFastCounter(klass, name, scope)

	def newFastHistogram(klass: Class[_], name: String, scope: String = null, biased: Boolean = false): FastHistogram =
		registry.newFastHistogram(klass, name, scope, biased)

	def newCachedFastHistogram(klass: Class[_], name: String, scope: String = null, biased: Boolean = false): FastHistogram =
		registry.newCachedFastHistogram(klass, name, scope, biased)
}