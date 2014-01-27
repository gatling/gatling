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
package io.gatling.core.action

import akka.actor.ActorRef
import io.gatling.core.akka.BaseActor
import io.gatling.core.controller.{ Controller, ForceTermination }
import io.gatling.core.feeder.{ Feeder, Record }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ Failure, FailureWrapper, Success, SuccessWrapper, Validation }

class SingletonFeed[T](val feeder: Feeder[T]) extends BaseActor {

	def receive = {
		case message: FeedMessage => feed(message.session, message.number, message.next)
	}

	def feed(session: Session, number: Expression[Int], next: ActorRef) {

		def translateRecord(record: Record[T], suffix: Int): Record[T] = record.map { case (key, value) => (key + suffix) -> value }

		def pollRecord(): Validation[Record[T]] = {
			if (!feeder.hasNext)
				"Feeder is now empty, stopping engine".failure
			else
				feeder.next.success
		}

		def injectRecords(numberOfRecords: Int): Validation[Session] =
			numberOfRecords match {
				case 1 =>
					pollRecord.map(session.setAll)
				case n if n > 0 =>
					val translatedRecords = Iterator.tabulate(n) { i =>
						pollRecord.map(translateRecord(_, i + 1))
					}.reduce {
						for (record1 <- _; record2 <- _) yield record1 ++ record2
					}
					translatedRecords.map(session.setAll)
				case n => (s"$n is not a valid number of records").failure
			}

		val newSession = number(session).flatMap(injectRecords) match {
			case Success(newSession) => newSession
			case Failure(message) =>
				logger.error(message)
				Controller ! ForceTermination(Some(new IllegalStateException(message)))
				session
		}

		next ! newSession
	}
}

case class FeedMessage(session: Session, number: Expression[Int], next: ActorRef)
