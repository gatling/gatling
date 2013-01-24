/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import scala.concurrent.Future
import scala.util.{ Failure, Success }

import com.excilys.ebi.gatling.core.action.{ BaseActor, system }
import com.excilys.ebi.gatling.core.result.message.Flush

import akka.actor.{ ActorRef, Props }
import akka.pattern.ask

object Terminator {

	private val terminator = system.actorOf(Props[Terminator])

	def init(latch: CountDownLatch, userCount: Int) {
		terminator ! Initialize(latch, userCount)
	}

	def registerDataWriter(dataWriter: ActorRef) {
		terminator ! RegisterDataWriter(dataWriter)
	}

	def endUser {
		terminator ! EndUser
	}

	def forceTermination {
		terminator ! ForceTermination
	}
}

class Terminator extends BaseActor {

	import context._

	/**
	 * The countdown latch that will be decreased when all message are written and all scenarios ended
	 */
	private var latch: CountDownLatch = _
	private var userCount: Int = _

	private var registeredDataWriters: List[ActorRef] = Nil

	def uninitialized: Receive = {

		case Initialize(latch, userCount) =>
			this.latch = latch
			this.userCount = userCount
			registeredDataWriters = Nil
			context.become(initialized)
	}

	def flush {
		Future.sequence(registeredDataWriters.map(_.ask(Flush)))
			.onComplete {
				case Success(_) =>
					latch.countDown
					context.unbecome
				case Failure(e) => error(e)
			}
	}

	def initialized: Receive = {

		case RegisterDataWriter(dataWriter: ActorRef) =>
			registeredDataWriters = dataWriter :: registeredDataWriters

		case EndUser =>
			userCount = userCount - 1
			if (userCount == 0) flush

		case ForceTermination => flush
	}

	def receive = uninitialized
}