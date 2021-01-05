/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.graphite.sender

import java.net.InetSocketAddress

import scala.concurrent.duration._

import io.gatling.commons.util.Clock
import io.gatling.graphite.message.GraphiteMetrics

import akka.io.{ IO, Tcp }

private[graphite] class TcpSender(
    remote: InetSocketAddress,
    maxRetries: Int,
    retryWindow: FiniteDuration,
    clock: Clock
) extends MetricsSender
    with TcpSenderFSM {

  import Tcp._

  // Initial ask for a connection to IO manager
  askForConnection()

  // Wait for answer from IO manager
  startWith(WaitingForConnection, DisconnectedData(new Retry(maxRetries, retryWindow, clock)))

  when(WaitingForConnection) {
    // Connection succeeded: proceed to running state
    case Event(_: Connected, DisconnectedData(failures)) =>
      unstashAll()
      val connection = sender()
      connection ! Register(self)
      goto(Running) using ConnectedData(connection, failures)

    // Connection failed: either stop if all retries are exhausted or retry connection
    case Event(CommandFailed(_: Connect), DisconnectedData(failures)) =>
      logger.info(s"Failed to connect to Graphite server located at: $remote")
      val newFailures = failures.newRetry

      stopIfLimitReachedOrContinueWith(newFailures) {
        scheduler.scheduleOnce(1.second)(askForConnection())
        stay() using DisconnectedData(newFailures)
      }

    case _ =>
      stash()
      stay()
  }

  when(Running) {
    // GraphiteDataWriter sent a metric, write to socket
    case Event(GraphiteMetrics(bytes), ConnectedData(connection, _)) =>
      logger.debug(s"Sending metrics to Graphite server located at: $remote")
      connection ! Write(bytes)
      stay()

    // Connection actor failed to send metric, log it as a failure
    case Event(CommandFailed(_: Write), data: ConnectedData) =>
      logger.debug(s"Failed to write to Graphite server located at: $remote, retrying...")
      val newFailures = data.retry.newRetry

      stopIfLimitReachedOrContinueWith(newFailures) {
        stay() using data.copy(retry = newFailures)
      }

    // Server quits unexpectedly, retry connection
    case Event(PeerClosed | ErrorClosed(_), data: ConnectedData) =>
      logger.info(s"Disconnected from Graphite server located at: $remote, retrying...")
      val newFailures = data.retry.newRetry

      stopIfLimitReachedOrContinueWith(newFailures) {
        scheduler.scheduleOnce(1.second)(askForConnection())
        goto(WaitingForConnection) using DisconnectedData(newFailures)
      }
  }

  when(RetriesExhausted) { case _ =>
    logger.debug("All connection/sending retries have been exhausted, ignore further messages")
    stay()
  }

  initialize()

  def askForConnection(): Unit =
    IO(Tcp) ! Connect(remote)

  def stopIfLimitReachedOrContinueWith(failures: Retry)(continueState: this.State) =
    if (failures.isLimitReached) goto(RetriesExhausted) using NoData
    else continueState
}

private[sender] class Retry private (maxRetryLimit: Int, retryWindow: FiniteDuration, retries: List[Long], clock: Clock) {

  def this(maxRetryLimit: Int, retryWindow: FiniteDuration, clock: Clock) =
    this(maxRetryLimit, retryWindow, Nil, clock)

  private def copyWithNewRetries(retries: List[Long]) =
    new Retry(maxRetryLimit, retryWindow, retries, clock)

  def newRetry: Retry = copyWithNewRetries(clock.nowMillis :: cleanupOldRetries)

  def isLimitReached = cleanupOldRetries.length >= maxRetryLimit

  private def cleanupOldRetries: List[Long] = {
    val now = clock.nowMillis
    retries.filterNot(_ < (now - retryWindow.toMillis))
  }
}
