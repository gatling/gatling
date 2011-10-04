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

import com.excilys.ebi.gatling.core.action.EndAction._
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import com.excilys.ebi.gatling.core.util.PathHelper._
import com.excilys.ebi.gatling.core.util.DateHelper._
import java.io.FileOutputStream
import java.io.File
import java.io.BufferedOutputStream
import java.io.OutputStreamWriter
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.CountDownLatch

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
  var runOn = StringUtils.EMPTY

  /**
   * Method called when this actor receives a message
   */
  def receive = {
    // If the message comes from an action
    case ActionInfo(scenarioName, userId, action, executionStartDate, executionDuration, resultStatus, resultMessage, groups) ⇒ {
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
        .append(groups.mkString("|", "|", "|")).append("\n")

      // Write the line in the file
      osw.write(strBuilder.toString)

      if (latch.getCount == 1 && self.dispatcher.mailboxSize(self) == 0) {
        // Closes the OutputStreamWriter
        osw.flush
        osw.close
        // Decrease the latch (should be at 0 here)
        latch.countDown
      }
    }

    // If the message is sent to initialize the writer
    case InitializeDataWriter(runOn, latch) ⇒ {
      // Initialize files and folders that will be used to write the logs
      val dir = new File(GATLING_RESULTS_FOLDER + "/" + printFileNameDate(runOn))
      dir.mkdir
      val file = new File(dir, GATLING_SIMULATION_LOG_FILE)

      osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file, true)))
      this.runOn = printResultDate(runOn)
      this.latch = latch
    }
  }
}