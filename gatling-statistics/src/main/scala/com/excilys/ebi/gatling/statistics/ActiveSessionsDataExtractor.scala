package com.excilys.ebi.gatling.statistics

import scala.collection.SortedMap
import com.excilys.ebi.gatling.core.log.Logging

import scala.io.Source
import scala.collection.immutable.{ HashSet, TreeMap }
import scala.collection.mutable.{ HashMap, MultiMap, Set => MSet }

import java.lang.String

class ActiveSessionsDataExtractor(val runOn: String) extends Logging {
  val formattedRunOn = (new StringBuilder(runOn)).insert(12, ":").insert(10, ":").insert(8, " ").insert(6, "-").insert(4, "-").toString

  def getResults: Map[String, Int] = {
    var lastTimeValue = formattedRunOn
    var nbActiveSessions = 0
    var lastResult: Tuple2[String, Int] = ("", 0)

    var results: List[Tuple2[String, Int]] = Nil

    val data: MultiMap[String, String] = new HashMap[String, MSet[String]] with MultiMap[String, String]

    logger.info("[Stats] reading from file: " + "gatling_" + runOn)
    for (line <- Source.fromFile(runOn + "/simulation.log", "utf-8").getLines) {
      line.split("\t") match {
        case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage) => {
          if (actionName == "End of scenario") {
            logger.debug("-1 ActiveSession")
            data.removeBinding(executionStartDate, userId)
          } else {
            logger.debug("+1 ActiveSession")
            data.addBinding(executionStartDate, userId)
          }
        }
        case _ => sys.error("Input file not well formatted")
      }
    }

    var sortedData: TreeMap[String, Int] = TreeMap.empty
    data.foreach {
      case (date, userSet) => sortedData = sortedData + (date -> userSet.size)
    }

    sortedData
  }
}