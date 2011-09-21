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
import com.excilys.ebi.gatling.statistics.presenter.DataPresenter
import com.excilys.ebi.gatling.statistics.presenter.ActiveSessionsDataPresenter

class ActiveSessionsDataExtractor extends DataExtractor[LinkedHashMap[String, ListBuffer[(String, Double)]]] {

  val maxResolution = 100

  val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  val executionWindowByScenarioAndUser = new LinkedHashMap[String, LinkedHashMap[String, (String, String)]]
  var minDate: String = null
  var maxDate: String = null

  override def onRow(runOn: String, scenarioName: String, userId: String, actionName: String, executionStartDate: String, executionDuration: String, resultStatus: String, resultMessage: String) {

    val executionWindowByUser = executionWindowByScenarioAndUser.get(scenarioName).getOrElse {
      val scenarioWindows = new LinkedHashMap[String, (String, String)]
      executionWindowByScenarioAndUser += scenarioName -> scenarioWindows
      scenarioWindows
    }

    val executionWindow = executionWindowByUser.get(userId).getOrElse {

      // create new window entry with end date 0
      val userWindow = (executionStartDate, "0");
      executionWindowByUser += (userId -> userWindow)
      userWindow
    }

    // update window end date
    executionWindowByUser += (userId -> (executionWindow._1, executionStartDate))

    updateLimits(executionStartDate)
  }

  private def updateLimits(executionStartDate: String) {
    if (minDate == null || minDate > executionStartDate) {
      minDate = executionStartDate
    }

    if (maxDate == null || maxDate < executionStartDate) {
      maxDate = executionStartDate
    }
  }

  def getResults: LinkedHashMap[String, ListBuffer[(String, Double)]] = {
    val countsByScenarioAndTime = new LinkedHashMap[String, ListBuffer[(String, Double)]]
    var globalCountByTime = new ListBuffer[(String, Double)]()

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

    countsByScenarioAndTime += "All scenarios" -> globalCountByTime

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