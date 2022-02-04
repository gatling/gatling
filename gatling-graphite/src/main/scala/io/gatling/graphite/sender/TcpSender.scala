/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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
import java.nio.BufferOverflowException

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import io.gatling.commons.util.Clock
import io.gatling.graphite.message.GraphiteMetrics

import akka.io.{IO, Tcp}
import akka.util.ByteString

private[graphite] final case class Ack(offset: Int) extends Tcp.Event

private[graphite] class TcpSender(
                                   remote: InetSocketAddress,
                                   maxRetries: Int,
                                   retryWindow: FiniteDuration,
                                   clock: Clock,
                                   bufferSize: Int
                                 ) extends MetricsSender
  with TcpSenderFSM {

  import Tcp._

  private val maxStored = bufferSize
  private val highWatermark = maxStored * 5 / 10
  private val lowWatermark = maxStored * 3 / 10

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
      goto(Running) using ConnectedData(connection, failures, 0, Nil, 0L, false, 0, 10, false)

    // Re-connection succeeded, flush outstanding messages and proceed to running state
    case Event(_: Connected, data: ConnectedData) =>
      unstashAll()
      val connection = sender()
      connection ! Register(self)
      goto(FlushingBuffer) using data.copy(connection = connection)

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
    case Event(GraphiteMetrics(bytes), data: ConnectedData) =>
      logger.info(s"Sending metrics to Graphite server located at: $remote")
      data.connection ! Write(bytes, Ack(currentOffset(data)))
      buffer(bytes, data) match {
        case Success(newData) => stay() using newData
        case _ => goto(BufferOverflow) using NoData
      }

    case Event(Ack(ack), data: ConnectedData) =>
      stay() using acknowledge(ack, data)

    // Connection actor failed to send metric, log it as a failure
    case Event(CommandFailed(Write(_, Ack(ack))), data: ConnectedData) =>
      logger.warn(s"Failed to write to Graphite server located at: $remote, buffering...")
      data.connection ! ResumeWriting
      goto(Buffering) using data.copy(nack = ack)

    // Server quits unexpectedly, retry connection
    case Event(PeerClosed | ErrorClosed(_), data: ConnectedData) =>
      logger.warn(s"Disconnected from Graphite server located at: $remote, retrying...")
      stopIfLimitReachedOrReconnect(data)
  }

  when(Buffering) {
    case Event(GraphiteMetrics(bytes), data: ConnectedData) =>
      buffer(bytes, data) match {
        case Success(data) => stay() using data
        case _ => goto(BufferOverflow) using NoData
      }

    case Event(WritingResumed, data: ConnectedData) =>
      writeFirst(data)
      stay() using data

    case Event(PeerClosed, data: ConnectedData) =>
      stay() using data.copy(peerClosed = true)

    case Event(Ack(ack), data: ConnectedData) if ack < data.nack =>
      stay() using acknowledge(ack, data)

    case Event(Ack(ack), data: ConnectedData) =>
      val newData = acknowledge(ack, data)
      if (newData.storage.nonEmpty) {
        if (newData.toAck > 0) {
          writeFirst(newData)
          stay() using newData.copy(toAck = newData.toAck - 1)
        } else {
          writeAll(newData)
          if (newData.peerClosed) {
            goto(FlushingBuffer) using newData.copy(peerClosed = false, toAck = 10)
          } else {
            goto(Running) using newData.copy(peerClosed = false, toAck = 10)
          }
        }
      } else if (newData.peerClosed) {
        stopIfLimitReachedOrReconnect(newData.copy(peerClosed = false, toAck = 10))
      }
      else goto(Running) using newData.copy(peerClosed = false, toAck = 10)
  }

  when(FlushingBuffer) {
    case Event(CommandFailed(_: Write), data: ConnectedData) =>
      data.connection ! ResumeWriting
      goto(AwaitingWritingResumed) using data

    case Event(Ack(ack), data: ConnectedData) =>
      val newData = acknowledge(ack, data)
      if (newData.storage.isEmpty) {
        goto(Running) using newData
      } else {
        stay() using newData
      }
  }

  when(AwaitingWritingResumed) {
    case Event(WritingResumed, data: ConnectedData) =>
      writeAll(data)
      goto(FlushingBuffer)

    case Event(Ack(ack), data: ConnectedData) =>
      stay() using acknowledge(ack, data)
  }

  when(RetriesExhausted) { case _ =>
    logger.debug("All connection/sending retries have been exhausted, ignoring further messages")
    stay()
  }

  when(BufferOverflow) { case _ =>
    logger.debug("Buffer overflow, ignoring further messages")
    stay()
  }

  initialize()

  protected def askForConnection(): Unit =
    IO(Tcp) ! Connect(remote)

  private def stopIfLimitReachedOrContinueWith(failures: Retry)(continueState: this.State) =
    if (failures.isLimitReached) goto(RetriesExhausted) using NoData
    else continueState

  private def stopIfLimitReachedOrReconnect(data: ConnectedData) = {
    val newFailures = data.retry.newRetry

    if (newFailures.isLimitReached) goto(RetriesExhausted) using NoData
    else {
      scheduler.scheduleOnce(1.second)(askForConnection())
      goto(WaitingForConnection) using data.copy(retry = newFailures)
    }
  }

  private def currentOffset(data: ConnectedData): Int = data.storageOffset + data.storage.size

  private def buffer(bytes: ByteString, data: ConnectedData): Try[ConnectedData] = {
    val newData = data.copy(storage = data.storage :+ bytes, stored = data.stored + bytes.size)
    logger.trace(s"Buffering, data stored: [${newData.stored}/$maxStored]")

    if (newData.stored > maxStored) {
      logger.warn(s"Dropping connection to [$remote] (buffer overflow)")
      Failure(new BufferOverflowException())
    } else if (newData.stored > highWatermark) {
      logger.debug(s"Suspending reading at ${currentOffset(newData)}")
      newData.connection ! SuspendReading
      Success(newData.copy(suspended = true))
    } else {
      Success(newData)
    }
  }

  private def acknowledge(ack: Int, data: ConnectedData): ConnectedData = {
    require(ack == data.storageOffset, s"Received wrong ack $ack at ${data.storageOffset}")
    require(data.storage.nonEmpty, s"Storage was empty at ack $ack")

    val size = data.storage(0).size
    val newData = data.copy(stored = data.stored - size, storageOffset = data.storageOffset + 1, storage = data.storage.drop(1))

    if (newData.suspended && newData.stored < lowWatermark) {
      logger.debug("Resuming reading")
      newData.connection ! ResumeReading
      newData.copy(suspended = false)
    } else {
      newData
    }
  }

  private def writeFirst(data: ConnectedData): Unit =
    data.connection ! Write(data.storage(0), Ack(data.storageOffset))

  private def writeAll(data: ConnectedData): Unit = {
    for ((bytes, i) <- data.storage.zipWithIndex)
      data.connection ! Write(bytes, Ack(data.storageOffset + i))
  }
}

private[sender] class Retry private(maxRetryLimit: Int, retryWindow: FiniteDuration, retries: List[Long], clock: Clock) {

  def this(maxRetryLimit: Int, retryWindow: FiniteDuration, clock: Clock) =
    this(maxRetryLimit, retryWindow, Nil, clock)

  private def copyWithNewRetries(retries: List[Long]) =
    new Retry(maxRetryLimit, retryWindow, retries, clock)

  def newRetry: Retry = copyWithNewRetries(clock.nowMillis :: cleanupOldRetries)

  def isLimitReached: Boolean = cleanupOldRetries.length >= maxRetryLimit

  private def cleanupOldRetries: List[Long] = {
    val now = clock.nowMillis
    retries.filterNot(_ < (now - retryWindow.toMillis))
  }
}
