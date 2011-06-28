package com.excilys.ebi.gatling.core.statistics.writer

import com.excilys.ebi.gatling.core.statistics.message.ActionInfo
import com.excilys.ebi.gatling.core.statistics.message.InitializeStatWriter

import java.io.FileWriter

import akka.actor.Actor.registry

class FileStatWriter extends StatWriter {
  var fw: FileWriter = null
  var numberOfRelevantActions = 0
  var numberOfRelevantActionsDone = 0
  var scenarioName = ""
  var runOn = ""

  def receive = {
    case ActionInfo(userId, action, executionStartTime, executionDuration, result) => {
      fw.write(runOn + "\t" + scenarioName + "\t" + userId + "\t" + action + "\t" + executionStartTime + "\t" + executionDuration + "\t" + result + "\n")
      numberOfRelevantActionsDone += 1
      if (numberOfRelevantActions == numberOfRelevantActionsDone && self.getMailboxSize == 0) {
        fw.close
        logger.debug("All scenarios finished, stoping actors")
        registry.shutdownAll
      }
    }
    case InitializeStatWriter(runOn, scenarioName, numberOfRelevantActions) => {
      fw = new FileWriter(runOn, true);
      this.runOn = runOn
      this.scenarioName = scenarioName
      this.numberOfRelevantActions = numberOfRelevantActions
    }
  }
}