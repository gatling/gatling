package com.excilys.ebi.gatling.statistics;

import scala.io.Source

import java.io.FileWriter

class DataWriter {
  def writeUsersStats(runOn: String) = {
    val fw = new FileWriter("gatling_stats_users_" + runOn + ".tsv", true)

    var lastTimeValue = runOn
    var nbUsersAtSameTime = 0

    for (line <- Source.fromFile("gatling_" + runOn, "utf-8").getLines) {
      line.split("\t") match {
        case Array(runOn, scenarioName, userId, action, executionStartDate, executionDuration, result) => {
          if (lastTimeValue != executionStartDate)
            fw.write(executionStartDate + "\t" + nbUsersAtSameTime + "\t" + scenarioName + "\n")
          nbUsersAtSameTime += 1
          lastTimeValue = executionStartDate
        }
        case _ => sys.error("Input file not well formatted")
      }
    }
  }
}
