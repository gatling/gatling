package com.excilys.ebi.gatling.statistics;

import com.excilys.ebi.gatling.core.log.Logging

import scala.io.Source
import scala.collection.immutable.HashSet

import java.lang.String

import java.io.FileWriter

class UsersStatsDataWriter(val runOn: String) extends Logging {

  val fw = new FileWriter("gatling_stats_users_" + runOn + ".tsv")
  val formattedRunOn = (new StringBuilder(runOn)).insert(12, ":").insert(10, ":").insert(8, " ").insert(6, "-").insert(4, "-").toString

  def writeStats = {
    var lastLineParts: Tuple8[String, String, String, String, String, String, String, String] = ("", "", "", "", "", "", "", "")
    var lastTimeValue = formattedRunOn
    var nbUsersAtSameTime = 0
    var nbRequestErrorsAtSameTime = 0
    var nbRequestsAtSameTime = 0
    var set: Set[String] = new HashSet[String]

    for (line <- Source.fromFile("gatling_" + runOn, "utf-8").getLines) {
      logger.info("[Stats] reading from file: " + "gatling_" + runOn)
      line.split("\t") match {
        case Array(runOn, scenarioName, userId, action, executionStartDate, executionDuration, resultStatus, resultMessage) => {
          logger.debug("[Stats] line has the right number of parts")

          // Adding or removing user for this second
          set =
            if (action == "End of scenario") { logger.debug("Removed userId because it ended"); set - userId }
            else { logger.debug("Added userId because it is running"); set + userId }
          // Counting number of Requests
          nbRequestsAtSameTime += 1
          // Counting number of Errors
          if (resultStatus == "KO") nbRequestErrorsAtSameTime += 1

          // Writing the line because we have all information for this second
          if (lastTimeValue != executionStartDate) writeLine(scenarioName, lastTimeValue, nbUsersAtSameTime, nbRequestsAtSameTime, nbRequestErrorsAtSameTime)

          // Initialization of values for next second
          nbUsersAtSameTime = set.size
          nbRequestErrorsAtSameTime = 0
          nbRequestsAtSameTime = 0
          lastTimeValue = executionStartDate

          // For the last print
          lastLineParts = (runOn, scenarioName, userId, action, executionStartDate, executionDuration, resultStatus, resultMessage)
        }
        case _ => sys.error("Input file not well formatted")
      }
    }

    writeLine(lastLineParts._2, lastLineParts._5, nbUsersAtSameTime, 0, 0)

    fw.close
  }

  def writeLine(scenarioName: String, executionDate: String, usersAtThisDate: Int, nbRequestAtSameTime: Int, nbRequestErrorsAtSameTime: Int) = {
    logger.info("[Stats] -- Writing Line")
    fw.write(executionDate + "\t" + usersAtThisDate + "\t" + scenarioName + "\n")
  }
}
