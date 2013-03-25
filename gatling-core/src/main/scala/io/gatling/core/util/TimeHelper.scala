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
package com.excilys.ebi.gatling.core.util

import java.lang.System.{ currentTimeMillis, nanoTime }

object TimeHelper {

	val currentTimeMillisReference = currentTimeMillis
	val nanoTimeReference = nanoTime

	def computeTimeMillisFromNanos(nanos: Long) = (nanos - nanoTimeReference) / 1000000 + currentTimeMillisReference

	def nowMillis = computeTimeMillisFromNanos(nanoTime)

	def nowSeconds = computeTimeMillisFromNanos(nanoTime) / 1000
}