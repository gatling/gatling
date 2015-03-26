/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.result.writer

import io.gatling.core.akka.BaseActor
import io.gatling.core.assertion.Assertion

case class InitDataWriter(totalNumberOfUsers: Int)

/**
 * Abstract class for all DataWriters
 *
 * These writers are responsible for writing the logs that will be read to
 * generate the statistics
 */
abstract class DataWriter extends BaseActor {

  def onInitialize(assertions: Seq[Assertion], run: RunMessage, scenarios: Seq[ShortScenarioDescription]): Boolean

  def onTerminate(): Unit

  def uninitialized: Receive = {
    case Init(assertions, runMessage, scenarios) =>
      logger.info("Initializing")
      val status = onInitialize(assertions, runMessage, scenarios)
      logger.info("Initialized")
      context.become(initialized)
      sender ! status

    case m: DataWriterMessage => logger.error(s"Can't handle $m when in uninitialized state, discarding")
  }

  def onMessage(message: LoadEventMessage): Unit

  def onFlush(): Unit

  def initialized: Receive = {

    case Flush => onFlush()

    case Terminate => try {
      onTerminate()
    } finally {
      context.become(terminated)
      sender ! true
    }

    case message: LoadEventMessage => onMessage(message)
  }

  def terminated: Receive = {
    case m => logger.info(s"Can't handle $m after being flush")
  }

  def receive = uninitialized
}
