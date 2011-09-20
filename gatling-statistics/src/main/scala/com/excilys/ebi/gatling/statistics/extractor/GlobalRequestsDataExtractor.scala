package com.excilys.ebi.gatling.statistics.extractor

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ResultStatus
import com.excilys.ebi.gatling.core.result.message.ResultStatus._
import com.excilys.ebi.gatling.core.util.PathHelper._

import scala.io.Source
import scala.collection.immutable.TreeMap
import scala.collection.mutable.HashMap

import java.lang.String

class GlobalRequestsDataExtractor(val runOn: String) extends Logging {
  val formattedRunOn = (new StringBuilder(runOn)).insert(12, ":").insert(10, ":").insert(8, " ").insert(6, "-").insert(4, "-").toString

  def getResults: List[(String, (Double, Double, Double))] = {

    val failureRequestData: HashMap[String, Double] = new HashMap[String, Double]
    val successRequestData: HashMap[String, Double] = new HashMap[String, Double]
    val allRequestData: HashMap[String, Double] = new HashMap[String, Double]

    logger.info("[Stats] reading from file: {}/{}", runOn, GATLING_SIMULATION_LOG_FILE)
    for (line <- Source.fromFile(GATLING_RESULTS_FOLDER + "/" + runOn + "/" + GATLING_SIMULATION_LOG_FILE, "utf-8").getLines) {
      line.split("\t") match {
        case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage) => {
          if (actionName startsWith "Request") {
            def inc = incrementInMap(executionStartDate)_

            try {
              ResultStatus.withName(resultStatus) match {
                case OK =>
                  inc(successRequestData)
                  inc(allRequestData)
                case KO =>
                  inc(failureRequestData)
                  inc(allRequestData)
              }
            } catch {
              case e => sys.error("Input file not well formated")
            }
          }
        }
        case _ => sys.error("Input file not well formatted")
      }
    }

    var data: List[(String, (Double, Double, Double))] = Nil

    allRequestData.foreach {
      case (date, nbRequests) =>
        def get = getInMap(date)_
        data = (date, (nbRequests, get(successRequestData), get(failureRequestData))) :: data
    }

    data
  }

  private def getInMap(date: String)(map: HashMap[String, Double]): Double = {
    map.get(date).getOrElse(0)
  }

  private def incrementInMap(executionStartDate: String)(map: HashMap[String, Double]) = {
    map(executionStartDate) =
      if (map.contains(executionStartDate))
        map(executionStartDate) + 1
      else
        1
  }
}