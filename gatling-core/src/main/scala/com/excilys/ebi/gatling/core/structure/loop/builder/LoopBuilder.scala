package com.excilys.ebi.gatling.core.structure.loop.builder
import com.excilys.ebi.gatling.core.context.Context
import java.util.concurrent.TimeUnit
import com.excilys.ebi.gatling.core.structure.builder.AbstractStructureBuilder
import com.excilys.ebi.gatling.core.structure.builder.ChainBuilder
import com.excilys.ebi.gatling.core.structure.loop.handler.builder.TimesLoopHandlerBuilder
import com.excilys.ebi.gatling.core.structure.loop.handler.builder.DurationLoopHandlerBuilder
import com.excilys.ebi.gatling.core.structure.loop.handler.builder.ConditionalLoopHandlerBuilder

class LoopBuilder[B <: AbstractStructureBuilder[B]](structureBuilder: B, chain: ChainBuilder, counterName: Option[String]) {
	def counterName(counterName: String) = new LoopBuilder[B](structureBuilder, chain, Some(counterName))

	def times(times: Int): B = new TimesLoopHandlerBuilder(structureBuilder, chain, times, counterName).build
	def during(durationValue: Int, durationUnit: TimeUnit): B = new DurationLoopHandlerBuilder(structureBuilder, chain, durationValue, durationUnit, counterName).build
	def during(durationValue: Int): B = during(durationValue, TimeUnit.SECONDS)
	def asLongAs(testFunction: Context => Boolean): B = new ConditionalLoopHandlerBuilder(structureBuilder, chain, testFunction, counterName).build
	def asLongAs(contextKey: String, value: String): B = asLongAs((c: Context) => c.getAttribute(contextKey) == value)
}