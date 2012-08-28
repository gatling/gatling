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
package com.excilys.ebi.gatling.metrics.reporting

import com.yammer.metrics.reporting.MetricDispatcher
import com.yammer.metrics.core.{MetricName, Metric}
import com.excilys.ebi.gatling.metrics.types.{FastHistogram, FastCounter}
import com.excilys.ebi.gatling.metrics.core.GatlingMetricsProcessor

class GatlingMetricDispatcher extends MetricDispatcher {

	def dispatch[T](metric: Metric, name: MetricName, processor: GatlingMetricsProcessor[T], context: T) {

		metric match {
			case fastCounter: FastCounter => processor.processFastCounter(name, fastCounter, context)
			case fastHistogram :  FastHistogram =>    processor.processFastHistogram(name, fastHistogram, context)
			case _ =>
		}
	}
}
