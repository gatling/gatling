package com.excilys.ebi.gatling.core.statistics.writer

import com.excilys.ebi.gatling.core.statistics.message.ActionInfo
import com.excilys.ebi.gatling.core.statistics.message.InitializeStatWriter

import java.io.FileWriter

import org.apache.commons.lang.time.FastDateFormat

import akka.actor.Actor.registry

class FileStatWriter extends StatWriter {
  var fw: FileWriter = null
  var numberOfRelevantActions = 0
  var numberOfRelevantActionsDone = 0
  var scenarioName = ""
  var runOn = ""

  val formatter: FastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd hh:mm:ss")
  val fileNameFormatter = FastDateFormat.getInstance("yyyyMMddhhmmss")

  def receive = {
    case ActionInfo(userId, action, executionStartDate, executionDuration, result) ⇒ {
      fw.write(
        runOn + "\t" + scenarioName + "\t" + userId + "\t" + action + "\t" + formatter.format(executionStartDate) + "\t" + executionDuration + "\t" + result + "\n")
      numberOfRelevantActionsDone += 1
      if (numberOfRelevantActions == numberOfRelevantActionsDone && self.getMailboxSize == 0) {
        fw.close
        logger.debug("All scenarios finished, stoping actors")
        registry.shutdownAll
      }
    }
    case InitializeStatWriter(runOn, scenarioName, numberOfRelevantActions) ⇒ {
      fw = new FileWriter("gatling_" + fileNameFormatter.format(runOn), true);
      this.runOn = formatter.format(runOn)
      this.scenarioName = scenarioName
      this.numberOfRelevantActions = numberOfRelevantActions
    }
  }
}