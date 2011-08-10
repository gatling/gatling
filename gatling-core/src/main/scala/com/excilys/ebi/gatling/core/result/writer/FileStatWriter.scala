package com.excilys.ebi.gatling.core.result.writer

import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter

import java.io.FileOutputStream
import java.io.File
import java.io.BufferedOutputStream

import org.apache.commons.lang.time.FastDateFormat

import akka.actor.Actor.registry

class FileDataWriter extends DataWriter {
  var bos: BufferedOutputStream = null

  var numberOfRelevantActions = 0
  var numberOfRelevantActionsDone = 0
  var scenarioName = ""
  var runOn = ""

  val formatter: FastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd hh:mm:ss")
  val fileNameFormatter = FastDateFormat.getInstance("yyyyMMddhhmmss")

  def receive = {
    case ActionInfo(scenarioName, userId, action, executionStartDate, executionDuration, resultStatus, resultMessage) ⇒ {
      bos.write((runOn + "\t" + scenarioName + "\t" + userId + "\t" + action + "\t" + formatter.format(executionStartDate) + "\t" + executionDuration + "\t" + resultStatus + "\t" + resultMessage + "\n").getBytes)
      numberOfRelevantActionsDone += 1
      if (numberOfRelevantActions == numberOfRelevantActionsDone && self.dispatcher.mailboxSize(self) == 0) {
        bos.flush
        bos.close
        logger.debug("All scenarios finished, stoping actors")
        registry.shutdownAll
      }
    }
    case InitializeDataWriter(runOn, numberOfRelevantActions) ⇒ {
      val dir = new File("results/" + fileNameFormatter.format(runOn))
      dir.mkdir
      val file = new File(dir, "simulation.log")

      bos = new BufferedOutputStream(new FileOutputStream(file, true))
      this.runOn = formatter.format(runOn)
      this.scenarioName = scenarioName
      this.numberOfRelevantActions = numberOfRelevantActions
    }
  }
}