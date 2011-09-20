package com.excilys.ebi.gatling.statistics.extractor

import scala.math._
import scala.collection.mutable.Map
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ListBuffer
import scala.io.Source
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import org.joda.time.Duration
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.PathHelper._

class ActiveSessionsDataExtractor(val runOn: String) extends Logging {

  val maxResolution = 100

  val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  def getResults: Map[String, ListBuffer[(String, Double)]] = {

    val executionWindowByScenarioAndUser = new LinkedHashMap[String, LinkedHashMap[String, (String, String)]]
    var minDate: String = null
    var maxDate: String = null

    logger.info("[Stats] reading from file: {}/{}", runOn, GATLING_SIMULATION_LOG_FILE)

    // Going through the specified log file
    for (line <- Source.fromFile(GATLING_RESULTS_FOLDER + "/" + runOn + "/" + GATLING_SIMULATION_LOG_FILE, "utf-8").getLines) {
      // Split each line by tabulation (As we get data from a TSV file)
      line.split("\t") match {
        // If we have a well formated result
        case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage) => {

          val executionWindowByUser = executionWindowByScenarioAndUser.get(scenarioName).getOrElse {
            val scenarioWindows = new LinkedHashMap[String, (String, String)]
            executionWindowByScenarioAndUser += scenarioName -> scenarioWindows
            scenarioWindows
          }

          val executionWindow = executionWindowByUser.get(userId).getOrElse {

            // this is a start date
            val userWindow = (executionStartDate, "0");
            executionWindowByUser += (userId -> userWindow)

            if (minDate == null || minDate > executionStartDate) {
              minDate = executionStartDate
            }

            userWindow
          }

          // Depending on the type of action, we add the session to the good map
          if (actionName == "End of scenario") {

            // this is an end date
            executionWindowByUser += (userId -> (executionWindow._1, executionStartDate))

            if (maxDate == null || maxDate < executionStartDate) {
              maxDate = executionStartDate
            }
          }
        }
        // Else, if the resulting data is not well formated print an error message
        case _ => sys.error("Input file not well formatted")
      }
    }

    // decompose execution window into 500 points

    val countsByScenarioAndTime = new LinkedHashMap[String, ListBuffer[(String, Double)]]()
    var globalCountByTime = new ListBuffer[(String, Double)]()
    countsByScenarioAndTime += "All scenarios" -> globalCountByTime

    getTimes(minDate, maxDate).foreach { time =>

      // iterate on scenarios
      var activeUsers = 0
      executionWindowByScenarioAndUser.foreach { scenarioAndWindowsByUser =>
        val (scenarioName, windowsByUser) = scenarioAndWindowsByUser

        var activeUsersByScenario = 0
        // iterate on users
        windowsByUser.foreach { windowByUser =>
          val (userId, (windowStart, windowEnd)) = windowByUser

          if (windowStart <= time && time < windowEnd) {
            activeUsers = activeUsers + 1
            activeUsersByScenario = activeUsersByScenario + 1
          }
        }

        val countsByTime = countsByScenarioAndTime.get(scenarioName).getOrElse {
          val counts = new ListBuffer[(String, Double)]()
          countsByScenarioAndTime += (scenarioName -> counts)
          counts
        }

        countsByTime += ((time, activeUsersByScenario))
      }

      globalCountByTime += ((time, activeUsers))
    }

    countsByScenarioAndTime
  }

  private def getTimes(minDate: String, maxDate: String) = {
    val start = DateTime.parse(minDate, dateTimeFormat);
    val end: DateTime = DateTime.parse(maxDate, dateTimeFormat);

    val stepMillis = getStepMillis(start, end)
    var current = start
    val times = new ListBuffer[String]()

    while (current.compareTo(end) < 0) {
      times += dateTimeFormat.print(current);
      current = current.plus(stepMillis)
    }

    times
  }

  private def getStepMillis(start: DateTime, end: DateTime) = {
    val between = new Duration(start, end);
    max(between.getMillis() / maxResolution, 1000);
  }
}