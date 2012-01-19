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
package com.excilys.ebi.gatling.core.result.writer

import java.io.{ OutputStreamWriter, FileOutputStream, BufferedOutputStream }
import java.util.concurrent.CountDownLatch
import scala.tools.nsc.io.{ File, Directory }
import com.excilys.ebi.gatling.core.config.GatlingFiles.{ simulationLogFile, resultFolder }
import com.excilys.ebi.gatling.core.result.message.{ InitializeDataWriter, ActionInfo }
import com.excilys.ebi.gatling.core.util.DateHelper.printFileNameDate
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE
import akka.actor.scala2ActorRef
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation if a tabulation separated values file
 */
class FileDataWriter extends DataWriter {

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
		case ActionInfo(scenarioName, userId, action, executionStartDate, executionDuration, endOfRequestSendingDate, startOfResponseReceivingDate, resultStatus, resultMessage) => {

			// Write the line in the file
			new ResultLine(runOn, scenarioName, userId, action, executionStartDate, executionDuration, endOfRequestSendingDate, startOfResponseReceivingDate, resultStatus, resultMessage).print(osw).append(END_OF_LINE)

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
			this.runOn = printFileNameDate(runOn)
			// Initialize files and folders that will be used to write the logs
			Directory(resultFolder(this.runOn)).createDirectory()

			osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(File(simulationLogFile(this.runOn)).jfile, true)))

			ResultLine.Headers.print(osw).append(END_OF_LINE)

			this.latch = latch
		}
	}
}
