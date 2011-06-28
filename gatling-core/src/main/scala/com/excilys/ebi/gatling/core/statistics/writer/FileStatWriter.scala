package com.excilys.ebi.gatling.core.statistics.writer

import com.excilys.ebi.gatling.core.statistics.message.ActionInfo
import com.excilys.ebi.gatling.core.statistics.message.NumberOfRelevantActions

import java.io.FileWriter
import java.util.Date

import akka.actor.Actor.registry

class FileStatWriter extends StatWriter {
  val fw = new FileWriter((new Date).toString, true);
  var numberOfRelevantActions = 0
  var numberOfRelevantActionsDone = 0

  def receive = {
    case ActionInfo(scenarioName, userId, actionSummary, executionTime) => {
      fw.write(scenarioName + "\t" + userId + "\t" + executionTime + "\t" + actionSummary + "\n")
      numberOfRelevantActionsDone += 1
      if (numberOfRelevantActions == numberOfRelevantActionsDone && self.getMailboxSize == 0) {
        fw.close
        logger.debug("All scenarios finished, stoping actors")
        registry.shutdownAll
      }
    }
    case NumberOfRelevantActions(n) => numberOfRelevantActions = n
  }
}