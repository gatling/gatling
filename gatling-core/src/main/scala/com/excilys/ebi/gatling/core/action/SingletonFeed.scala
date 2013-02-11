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
package com.excilys.ebi.gatling.core.action

import akka.actor.ActorRef
import com.excilys.ebi.gatling.core.feeder.{ Feeder, Record }
import com.excilys.ebi.gatling.core.result.terminator.Terminator
import com.excilys.ebi.gatling.core.session.{ Expression, Session }

import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz.ToValidationV

class SingletonFeed[T](val feeder: Feeder[T], val number: Expression[Int]) extends BaseActor {
	def receive = {
		case message: FeedMessage => execute(message.session, message.next)
	}

	def execute(session: Session, next: ActorRef) {

		def translateRecord(record: Record[T], suffix: Int): Record[T] = record.map { case (key, value) => (key + suffix) -> value }

		def pollRecord(): Record[T] = {
			if (!feeder.hasNext) {
				error("Feeder is now empty, stopping engine")
				Terminator.forceTermination
			}

			feeder.next
		}

		def injectRecords(session: Session, numberOfRecords: Int): Validation[String, Session] =
			numberOfRecords match {
				case 1 => session.set(pollRecord).success
				case n if n > 0 =>
					val translatedRecords = for (i <- 1 to n) yield translateRecord(pollRecord, i)
					val mergedRecord = translatedRecords.reduce(_ ++ _)
					session.set(mergedRecord).success
				case n => (s"$n is not a valid number of records").failure
			}

		val newSession = number(session).flatMap(injectRecords(session, _)) match {
			case Success(newSession) => newSession
			case Failure(message) => error(message); session
		}

		next ! newSession
	}
}

case class FeedMessage(session: Session, next: ActorRef)