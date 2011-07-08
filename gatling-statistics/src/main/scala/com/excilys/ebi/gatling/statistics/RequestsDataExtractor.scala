package com.excilys.ebi.gatling.statistics

import scala.collection.SortedMap
import com.excilys.ebi.gatling.core.log.Logging

import scala.io.Source
import scala.collection.immutable.{ HashSet, TreeMap }
import scala.collection.mutable.{ HashMap, MultiMap, Set => MSet }

import java.lang.String

class RequestsDataExtractor(val runOn: String) extends Logging {
  val formattedRunOn = (new StringBuilder(runOn)).insert(12, ":").insert(10, ":").insert(8, " ").insert(6, "-").insert(4, "-").toString

  def getResults: Map[String, Tuple3[Int, Int, Int]] = {
    var lastTimeValue = formattedRunOn
    var nbActiveSessions = 0
    var lastResult: Tuple2[String, Int] = ("", 0)

    var results: List[Tuple2[String, Int]] = Nil

    val failureRequestData: HashMap[String, Int] = new HashMap[String, Int]
    val successRequestData: HashMap[String, Int] = new HashMap[String, Int]
    val allRequestData: HashMap[String, Int] = new HashMap[String, Int]

    logger.info("[Stats] reading from file: " + "gatling_" + runOn)
    for (line <- Source.fromFile(runOn + "/simulation.log", "utf-8").getLines) {
      line.split("\t") match {
        case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage) => {
          if (actionName startsWith "Request") {
            def inc = incrementInMap(executionStartDate)_

            resultStatus match {
              case "OK" =>
                inc(successRequestData)
                inc(allRequestData)
              case "KO" =>
                inc(failureRequestData)
                inc(allRequestData)
              case _ => sys.error("Result Status not well formatted")
            }
          }
        }
        case _ => sys.error("Input file not well formatted")
      }
    }

    var sortedData: TreeMap[String, Tuple3[Int, Int, Int]] = TreeMap.empty

    allRequestData.foreach {
      case (date, nbRequests) =>
        def get = getInMap(date)_
        sortedData = sortedData + (date -> (nbRequests, get(successRequestData), get(failureRequestData)))
    }

    sortedData
  }

  private def getInMap(date: String)(map: HashMap[String, Int]): Int = {
    map.get(date) match {
      case Some(i) => i
      case None => 0
    }
  }

  private def incrementInMap(executionStartDate: String)(map: HashMap[String, Int]) = {
    map(executionStartDate) =
      if (map.contains(executionStartDate))
        map(executionStartDate) + 1
      else
        1
  }
}