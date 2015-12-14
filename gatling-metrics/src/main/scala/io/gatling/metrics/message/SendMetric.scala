/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.metrics.message

import java.nio.charset.StandardCharsets.UTF_8

import akka.actor.ActorRef
import io.gatling.commons.util.StringHelper

import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging

import scala.util.Random

private[metrics] case class GraphiteMetrics(byteString: ByteString)

private[metrics] object GraphiteMetrics extends StrictLogging {

  def apply(pathValuePairs: Iterator[(String, Long)], epoch: Long): GraphiteMetrics = {

    val sb = StringHelper.stringBuilder()
    pathValuePairs.foreach {
      case (path, value) =>
        sb.append(path).append(' ').append(value).append(' ').append(epoch).append('\n')
    }
    if (logger.underlying.isDebugEnabled)
      logger.debug(s"GraphiteMetrics=${sb.toString}")
    GraphiteMetrics(ByteString(sb.toString, UTF_8.name))
  }
}

private[metrics] case class StatsdMetrics(byteString: ByteString)

private[metrics] object StatsdMetrics extends StrictLogging {

  private val rand = new Random()

  object StatsDProtocol {
    val TIMING_METRIC = "ms"
    val COUNTER_METRIC = "c"
    val GAUGE_METRIC = "g"

    /**
     * @return Returns a string that conforms to the StatsD protocol:
     *         KEY:VALUE|METRIC or KEY:VALUE|METRIC|@SAMPLE_RATE
     */
    def stat(key: String, value: String, metric: String, sampleRate: Double) = {
      val sampleRateString = if (sampleRate < 1) "|@" + sampleRate else ""
      key + ":" + value + "|" + metric + sampleRateString
    }
  }

  def timing(actorRef: ActorRef, key: String, value: Int, sampleRate: Double = 1.0) = {
    send(actorRef, key, value.toString, StatsDProtocol.TIMING_METRIC, sampleRate)
  }

  def decrementCount(actorRef: ActorRef, key: String, magnitude: Int = -1, sampleRate: Double = 1.0) = {
    incrementCount(actorRef, key, magnitude, sampleRate)
  }

  def incrementCount(actorRef: ActorRef, key: String, magnitude: Int = 1, sampleRate: Double = 1.0) = {
    send(actorRef, key, magnitude.toString, StatsDProtocol.COUNTER_METRIC, sampleRate)
  }

  def decrementGauge(actorRef: ActorRef, key: String, value: String = "-1", sampleRate: Double = 1.0) = {
    gauge(actorRef, key, value, sampleRate)
  }

  def incrementGauge(actorRef: ActorRef, key: String, value: String = "+1", sampleRate: Double = 1.0) = {
    gauge(actorRef, key, value, sampleRate)
  }

  def gauge(actorRef: ActorRef, key: String, value: String = "1", sampleRate: Double = 1.0) = {
    send(actorRef, key, value, StatsDProtocol.GAUGE_METRIC, sampleRate)
  }

  /**
   * Checks the sample rate and sends the stat to the actor if it passes
   */
  private def send(actorRef: ActorRef, key: String, value: String, metric: String, sampleRate: Double): Boolean = {
    if (sampleRate >= 1 || rand.nextDouble <= sampleRate) {
      actorRef ! apply(StatsDProtocol.stat(key, value, metric, sampleRate))
      true
    } else {
      false
    }
  }

  def apply(stat: String): StatsdMetrics = {
    if (logger.underlying.isDebugEnabled)
      logger.debug(s"statsdMetrics=${stat}")
    StatsdMetrics(ByteString(stat, UTF_8.name))
  }
}