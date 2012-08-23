/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.result.terminator

import java.util.concurrent.CountDownLatch
import com.excilys.ebi.gatling.core.action.system
import akka.actor.{ Actor, Props }
import grizzled.slf4j.Logging

object Terminator {

	private val terminator = system.actorOf(Props[Terminator])

	def init(latch: CountDownLatch) {
		terminator ! Initialize(latch)
	}

	def registerDataWriter {
		terminator ! RegisterDataWriter
	}

	def terminateDataWriter {
		terminator ! TerminateDataWriter
	}
}

class Terminator extends Actor with Logging {

	/**
	 * The countdown latch that will be decreased when all message are written and all scenarios ended
	 */
	private var latch: CountDownLatch = _

	private var registered = 0

	def uninitialized: Receive = {

		case Initialize(latch) =>
			this.latch = latch
			context.become(initialized)
	}

	def initialized: Receive = {

		case RegisterDataWriter =>
			registered = registered + 1

		case TerminateDataWriter =>
			registered = registered - 1
			if (registered == 0) {
				latch.countDown
				context.unbecome
			}
	}

	def receive = uninitialized
}