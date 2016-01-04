/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.metrics.sender

import java.net.InetSocketAddress

import scala.concurrent.duration._

import io.gatling.core.akka.BaseActor
import io.gatling.core.config._

import akka.actor.{ Props, Stash }

private[metrics] object MetricsSender {

  def props(configuration: GatlingConfiguration): Props = {
    val remote = new InetSocketAddress(configuration.data.graphite.host, configuration.data.graphite.port)
    configuration.data.graphite.protocol match {
      case Tcp => Props(new TcpSender(remote, 5, 5.seconds))
      case Udp => Props(new UdpSender(remote))
    }
  }
}

private[metrics] abstract class MetricsSender extends BaseActor with Stash
