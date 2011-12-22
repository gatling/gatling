package com.excilys.ebi.gatling.core.feeder
import com.twitter.util.RingBuffer
import java.util.concurrent.atomic.AtomicInteger

class CircularFeeder(feederSource: FeederSource) extends Feeder(feederSource) {

	private val bufferSize = feederSource.values.size
	private val currentIndex = new AtomicInteger(0)

	val values = new RingBuffer[Map[String, String]](bufferSize)

	values ++= feederSource.values

	def next: Map[String, String] = values(currentIndex.getAndAdd(1) % bufferSize)
}