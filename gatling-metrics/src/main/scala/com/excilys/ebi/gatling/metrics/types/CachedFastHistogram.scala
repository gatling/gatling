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
package com.excilys.ebi.gatling.metrics.types

import com.yammer.metrics.stats.{Snapshot, Sample}

class CachedFastHistogram(sample: Sample) extends FastHistogram(sample) {

	private var snapshotCache: Snapshot = super.getSnapshot
	private var minCache = .0
	private var maxCache = .0
	private var sumCache = .0
	private var countCache = 0L
	private var stdDevCache = .0
	private var meanCache = .0

	override def clear() {
		snapshotCache = super.getSnapshot
		minCache = super.getMin
		maxCache = super.getMax
		sumCache = super.getSum
		countCache = super.getCount
		stdDevCache = super.getStdDev
		meanCache = super.getMean
		super.clear()
	}

	override def getMax = maxCache

	override def getMin = minCache

	override def getMean = meanCache

	override def getStdDev = stdDevCache

	override def getSum = sumCache

	override def getCount = countCache

	override def getSnapshot = snapshotCache
}
