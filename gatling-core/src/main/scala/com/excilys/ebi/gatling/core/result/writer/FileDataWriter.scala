/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.result.writer

import java.io.{ OutputStreamWriter, FileOutputStream, File, BufferedOutputStream }
import java.util.concurrent.CountDownLatch

import scala.tools.nsc.io.Path.string2path

import com.excilys.ebi.gatling.core.result.message.{ InitializeDataWriter, ActionInfo }
import com.excilys.ebi.gatling.core.util.DateHelper.{ printResultDate, printFileNameDate }
import com.excilys.ebi.gatling.core.config.GatlingFiles.{ GATLING_SIMULATION_LOG_FILE, GATLING_RESULTS_FOLDER }
import com.excilys.ebi.gatling.core.util.StringHelper.{ END_OF_LINE, EMPTY }

import FileDataWriter.{ GROUPS_SUFFIX, GROUPS_SEPARATOR, GROUPS_PREFIX }
import akka.actor.scala2ActorRef

/**
 * FileDataWriter class companion
 */
object FileDataWriter {
	val GROUPS_PREFIX = "("

	val GROUPS_SUFFIX = ")"

	val GROUPS_SEPARATOR = ","
}

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation if a tabulation separated values file
 */
class FileDataWriter extends DataWriter {

	import FileDataWriter._

	/**
	 * The OutputStreamWriter used to write to files
	 */
	var osw: OutputStreamWriter = null
	/**
	 * The countdown latch that will be decreased when all messaged are written and all scenarios ended
	 */
	var latch: CountDownLatch = null
	/**
	 * The date on which the simulation started
	 */
	var runOn = EMPTY

	/**
	 * Method called when this actor receives a message
	 */
	def receive = {
		// If the message comes from an action
		case ActionInfo(scenarioName, userId, action, executionStartDate, executionDuration, resultStatus, resultMessage, groups) => {
			// Builds the line to be written
			val strBuilder = new StringBuilder
			strBuilder.append(runOn).append("\t")
				.append(scenarioName).append("\t")
				.append(userId).append("\t")
				.append(action).append("\t")
				.append(printResultDate(executionStartDate)).append("\t")
				.append(executionDuration).append("\t")
				.append(resultStatus).append("\t")
				.append(resultMessage).append("\t")
				.append(groups.mkString(GROUPS_PREFIX, GROUPS_SEPARATOR, GROUPS_SUFFIX)).append(END_OF_LINE)

			// Write the line in the file
			osw.write(strBuilder.toString)

			if (latch.getCount == 1 && self.dispatcher.mailboxSize(self) == 0) {
				try {
					// Closes the OutputStreamWriter
					osw.flush
				} finally {
					// Decrease the latch (should be at 0 here)
					latch.countDown
					osw.close
				}
			}
		}

		// If the message is sent to initialize the writer
		case InitializeDataWriter(runOn, latch) => {
			// Initialize files and folders that will be used to write the logs
			val dir = (GATLING_RESULTS_FOLDER / printFileNameDate(runOn)).jfile
			dir.mkdir
			val file = new File(dir, GATLING_SIMULATION_LOG_FILE)

			osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file, true)))
			this.runOn = printResultDate(runOn)
			this.latch = latch
		}
	}
}
