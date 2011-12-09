package com.excilys.ebi.gatling.core.feeder

object FeederBuilder {
	implicit def feederBuilderToFeeder(builder: FeederBuilder[_]) = builder.asQueue
}
abstract class FeederBuilder[B <: FeederSource] {

	protected def sourceInstance: B

	def asQueue = new QueueFeeder(sourceInstance)
}