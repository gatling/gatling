package com.excilys.ebi.gatling.statistics.extractor

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.PathHelper._
import com.excilys.ebi.gatling.core.action.EndAction._

import com.excilys.ebi.gatling.statistics.result.DetailsRequestsDataResult

import scala.io.Source
import scala.collection.immutable.TreeMap
import scala.math._

class DetailsRequestsDataExtractor(val runOn: String) extends Logging {

  def getResults: Map[String, DetailsRequestsDataResult] = {
    val extractedInformation = extractFromFile
    computeStatistics(extractedInformation)
  }

  private def extractFromFile: Map[String, List[(String, Double)]] = {
    var extractedValues: Map[String, List[(String, Double)]] = TreeMap.empty
    for (line <- Source.fromFile(GATLING_RESULTS_FOLDER + "/" + runOn + "/" + GATLING_SIMULATION_LOG_FILE, "utf-8").getLines) {
      // Split each line by tabulation (As we get data from a TSV file)
      line.split("\t") match {
        // If we have a well formated result
        case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage) => {
          if (actionName != END_OF_SCENARIO)
            extractedValues = extractedValues + (actionName ->
              ((executionStartDate, executionDuration.toDouble) :: extractedValues.getOrElse(actionName, Nil)))
        }
        // Else, if the resulting data is not well formated print an error message
        case _ => sys.error("Input file not well formatted")
      }
    }
    extractedValues
  }

  private def computeStatistics(extractedInformation: Map[String, List[(String, Double)]]): Map[String, DetailsRequestsDataResult] = {
    var results: Map[String, DetailsRequestsDataResult] = Map.empty
    extractedInformation.foreach {
      case (requestName, responseTimes) =>
        val nbOfElements = responseTimes.size.asInstanceOf[Double]
        val min = responseTimes.minBy(_._2)._2
        val max = responseTimes.maxBy(_._2)._2
        val medium = responseTimes.map(entry => entry._2).sum / nbOfElements
        val standardDeviation = sqrt(responseTimes.map(entry => (pow(entry._2 - medium, 2))).sum / nbOfElements)

        var responseTimesToNumberOfRequests: Map[Double, Double] = new TreeMap
        responseTimes.map {
          dateAndResponseTime =>
            responseTimesToNumberOfRequests += (dateAndResponseTime._2 -> (responseTimesToNumberOfRequests.get(dateAndResponseTime._2).getOrElse(0D) + 1))
        }
        val sortedResponseTimes = responseTimesToNumberOfRequests.keys.toList
        val numberOfRequests = responseTimesToNumberOfRequests.values.toList

        val result = new DetailsRequestsDataResult(responseTimes.size, responseTimes.reverse, (sortedResponseTimes, numberOfRequests), min, max, medium, standardDeviation)
        results = results + (requestName -> result)
    }
    results
  }
}