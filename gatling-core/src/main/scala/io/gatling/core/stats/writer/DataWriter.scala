/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.core.stats.writer

import scala.util.control.NonFatal

import io.gatling.core.actor.{ Actor, Behavior }

private[gatling] trait DataWriterData

/**
 * Abstract class for all DataWriters
 *
 * These writers are responsible for writing the logs that will be read to generate the statistics
 */
private[gatling] abstract class DataWriter[T <: DataWriterData](name: String) extends Actor[DataWriterMessage](name) {

  def onInit(): T

  def onFlush(data: T): Unit

  def onCrash(cause: String, data: T): Unit

  def onStop(data: T): Unit

  def onMessage(message: DataWriterMessage.LoadEvent, data: T): Unit

  override def init(): Behavior[DataWriterMessage] = {
    case DataWriterMessage.Init =>
      logger.info("Initializing")
      try {
        val newState = onInit()
        logger.info("Initialized")
        become(initialized(newState))
      } catch {
        case NonFatal(e) =>
          logger.error(s"DataWriter ${getClass.getName} failed to initialize", e)
          die
      }

    case msg => dieOnUnexpected(msg)
  }

  private def initialized(data: T): Behavior[DataWriterMessage] = {
    case DataWriterMessage.Flush =>
      onFlush(data)
      stay

    case DataWriterMessage.Stop(stopPromise) =>
      onStop(data)
      stopPromise.trySuccess(())
      die

    case DataWriterMessage.Crash(cause) =>
      onCrash(cause, data)
      die

    case message: DataWriterMessage.LoadEvent =>
      onMessage(message, data)
      stay

    case msg => dieOnUnexpected(msg)
  }
}
