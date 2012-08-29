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

import com.excilys.ebi.gatling.metrics.core.SampleType.{ Biased, Uniform }
import com.excilys.ebi.gatling.metrics.types.{ ClearedFastCounter, ClearedFastHistogram, FastCounter, FastHistogram }
import com.yammer.metrics.core.{ Clock, MetricsRegistry }

class GatlingMetricsRegistry(clock: Clock = Clock.defaultClock()) extends MetricsRegistry(clock) {

	def newFastCounter(klass: Class[_], name: String, scope: String = null): FastCounter =
		getOrAdd(createName(klass, name, scope), new FastCounter)

	def newClearedFastCounter(klass: Class[_], name: String, scope: String = null): ClearedFastCounter =
		getOrAdd(createName(klass, name, scope), new ClearedFastCounter)

	def newFastHistogram(klass: Class[_], name: String, scope: String = null, biased: Boolean = false): FastHistogram =
		getOrAdd(createName(klass, name, scope), new FastHistogram(if (biased) Biased.newSample else Uniform.newSample))

	def newClearedFastHistogram(klass: Class[_], name: String, scope: String = null, biased: Boolean = false): ClearedFastHistogram =
		getOrAdd(createName(klass, name, scope), new ClearedFastHistogram(if (biased) Biased.newSample else Uniform.newSample))

}
