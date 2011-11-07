/**
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
package com.excilys.ebi.gatling.core.structure.loop
import com.excilys.ebi.gatling.core.context.Context
import java.util.concurrent.TimeUnit
import com.excilys.ebi.gatling.core.structure.AbstractStructureBuilder
import com.excilys.ebi.gatling.core.structure.ChainBuilder
import com.excilys.ebi.gatling.core.structure.loop.handler.TimesLoopHandlerBuilder
import com.excilys.ebi.gatling.core.structure.loop.handler.DurationLoopHandlerBuilder
import com.excilys.ebi.gatling.core.structure.loop.handler.ConditionalLoopHandlerBuilder

class LoopBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B, chain: ChainBuilder, counterName: Option[String]) {

	def counterName(counterName: String) = new LoopBuilder[B](structureBuilder, chain, Some(counterName))

	def times(times: Int): B = new TimesLoopHandlerBuilder(structureBuilder, chain, times, counterName).build

	def during(durationValue: Int, durationUnit: TimeUnit): B = new DurationLoopHandlerBuilder(structureBuilder, chain, durationValue, durationUnit, counterName).build

	def during(durationValue: Int): B = during(durationValue, TimeUnit.SECONDS)

	def asLongAs(testFunction: Context => Boolean): B = new ConditionalLoopHandlerBuilder(structureBuilder, chain, testFunction, counterName).build

	def asLongAs(contextKey: String, value: String): B = asLongAs((c: Context) => c.getAttribute(contextKey) == value)
}