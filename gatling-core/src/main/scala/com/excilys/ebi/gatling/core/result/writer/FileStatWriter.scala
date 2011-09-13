package com.excilys.ebi.gatling.core.result.writer

import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import java.io.FileOutputStream
import java.io.File
import java.io.BufferedOutputStream
import java.io.OutputStreamWriter
import org.apache.commons.lang3.time.FastDateFormat
import akka.actor.Actor.registry
import org.apache.commons.lang3.StringUtils

class FileDataWriter extends DataWriter {
  var osw: OutputStreamWriter = null

  var numberOfRelevantActions = 0
  var numberOfRelevantActionsDone = 0
  var runOn = StringUtils.EMPTY

  val formatter: FastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss")
  val fileNameFormatter = FastDateFormat.getInstance("yyyyMMddHHmmss")

  def receive = {
    case ActionInfo(scenarioName, userId, action, executionStartDate, executionDuration, resultStatus, resultMessage) ⇒ {
      val strBuilder = new StringBuilder
      strBuilder.append(runOn).append("\t")
        .append(scenarioName).append("\t")
        .append(userId).append("\t")
        .append(action).append("\t")
        .append(formatter.format(executionStartDate)).append("\t")
        .append(executionDuration).append("\t")
        .append(resultStatus).append("\t")
        .append(resultMessage).append("\n")

      osw.write(strBuilder.toString)
      numberOfRelevantActionsDone += 1
      if (numberOfRelevantActions == numberOfRelevantActionsDone && self.dispatcher.mailboxSize(self) == 0) {
        osw.flush
        osw.close
        logger.debug("All scenarios finished, stoping actors")
        registry.shutdownAll
      }
    }
    case InitializeDataWriter(runOn, numberOfRelevantActions) ⇒ {
      val dir = new File("results/" + fileNameFormatter.format(runOn))
      dir.mkdir
      val file = new File(dir, "simulation.log")

      osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file, true)))
      this.runOn = formatter.format(runOn)
      this.numberOfRelevantActions = numberOfRelevantActions
    }
  }
}