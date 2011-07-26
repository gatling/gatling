package com.excilys.ebi.gatling.statistics.extractor

import scala.collection.SortedMap
import com.excilys.ebi.gatling.core.log.Logging

import scala.io.Source
import scala.collection.immutable.{ TreeMap, TreeSet }
import scala.collection.mutable.{ HashMap, MultiMap, Set => MSet }

import java.lang.String

class ActiveSessionsDataExtractor(val runOn: String) extends Logging {
  val formattedRunOn = (new StringBuilder(runOn)).insert(12, ":").insert(10, ":").insert(8, " ").insert(6, "-").insert(4, "-").toString

  def getResults: List[(String, Int)] = {
    val activeSessions: MultiMap[String, String] = new HashMap[String, MSet[String]] with MultiMap[String, String]
    val deadSessions: MultiMap[String, String] = new HashMap[String, MSet[String]] with MultiMap[String, String]

    logger.info("[Stats] reading from file: " + "gatling_" + runOn)

    // Going through the specified log file
    for (line <- Source.fromFile("results/" + runOn + "/simulation.log", "utf-8").getLines) {
      // Split each line by tabulation (As we get data from a TSV file)
      line.split("\t") match {
        // If we have a well formated result
        case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage) => {
          // Adding a dummy value to set a new entry for executionStartDate
          activeSessions.addBinding(executionStartDate, "0")

          // Depending on the type of action, we add the session to the good map
          if (actionName == "End of scenario") {
            deadSessions.addBinding(executionStartDate, userId)
          } else {
            activeSessions.addBinding(executionStartDate, userId)
          }
        }
        // Else, if the resulting data is not well formated print an error message
        case _ => sys.error("Input file not well formatted")
      }
    }

    // Getting all dates sorted
    var executionStartDates: TreeSet[String] = TreeSet.empty
    activeSessions.keySet.foreach { key =>
      executionStartDates = executionStartDates + key
    }

    // Adding the missing active/dead sessions to the different executionStartDate
    var lastDate: String = formattedRunOn
    executionStartDates.map { startDate =>
      if (startDate != formattedRunOn) {
        activeSessions.get(lastDate).map { set =>
          set.map { userId =>
            activeSessions.addBinding(startDate, userId)
          }
          deadSessions.get(startDate).map { set =>
            set.map { userId =>
              activeSessions.removeBinding(startDate, userId)
            }
          }
        }
      }
      lastDate = startDate
    }

    // Counting active sessions by starDate and returning result
    var results: List[(String, Int)] = Nil
    executionStartDates.foreach { startDate =>
      results = (startDate, (activeSessions.get(startDate).get.size - 1)) :: results
    }
    results
  }
}