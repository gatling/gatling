package com.excilys.ebi.gatling.core.feeder
import scala.collection.mutable.Queue

class QueueFeeder(feederSource: FeederSource) extends Feeder(feederSource) {

	val values = Queue(feederSource.values: _*)

	def next: Map[String, String] = {
		values.dequeue
	}
}