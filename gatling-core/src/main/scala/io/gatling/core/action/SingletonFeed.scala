/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.action

import io.gatling.core.feeder.{ Feeder, Record }
import io.gatling.core.result.terminator.Terminator
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ Failure, FailureWrapper, Success, SuccessWrapper, Validation }

import akka.actor.ActorRef

class SingletonFeed[T](val feeder: Feeder[T]) extends BaseActor {

	def receive = {
		case message: FeedMessage => feed(message.session, message.number, message.next)
	}

	def feed(session: Session, number: Expression[Int], next: ActorRef) {

		def translateRecord(record: Record[T], suffix: Int): Record[T] = record.map { case (key, value) => (key + suffix) -> value }

		def pollRecord(): Record[T] = {
			if (!feeder.hasNext) {
				logger.error("Feeder is now empty, stopping engine")
				Terminator.forceTermination
			}

			feeder.next
		}

		def injectRecords(numberOfRecords: Int): Validation[Session] =
			numberOfRecords match {
				case 1 => session.set(pollRecord).success
				case n if n > 0 =>
					val translatedRecords = for (i <- 1 to n) yield translateRecord(pollRecord, i)
					val mergedRecord = translatedRecords.reduce(_ ++ _)
					session.set(mergedRecord).success
				case n => (s"$n is not a valid number of records").failure
			}

		val newSession = number(session).flatMap(injectRecords) match {
			case Success(newSession) => newSession
			case Failure(message) => logger.error(message); session
		}

		next ! newSession
	}
}

case class FeedMessage(session: Session, number: Expression[Int], next: ActorRef)