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
package com.excilys.ebi.gatling.core.structure

import com.excilys.ebi.gatling.core.action.builder.SessionHookBuilder
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.result.terminator.Terminator
import com.excilys.ebi.gatling.core.session.{ Expression, Session }

import grizzled.slf4j.Logging
import scalaz.{ Success, Validation }
import scalaz.Failure
import scalaz.Scalaz.ToValidationV

trait Feeds[B] extends Execs[B] with Logging {

	/**
	 * Method used to load data from a feeder in the current scenario
	 *
	 * @param feeder the feeder from which the values will be loaded
	 * @param number the number of records to be polled (default 1)
	 */
	def feed(feeder: Feeder[_], number: Expression[Int] = Expression.wrap(1)): B = {

		type Record = Map[String, Any]

		def translateRecord(record: Record, suffix: Int): Record = record.map { case (key, value) => (key + suffix) -> value }

		def pollRecord(): Record = {
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
				case n => (n + " is not a valid number of records").failure
			}

		val byPass = new SessionHookBuilder(session =>
			number(session).flatMap(injectRecords(session, _)) match {
				case Success(newSession) => newSession
				case Failure(message) => error(message); session
			})

		newInstance(byPass :: actionBuilders)
	}
}