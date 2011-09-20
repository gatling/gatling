package com.excilys.ebi.gatling.statistics.extractor

import scala.collection.SortedMap
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.PathHelper._

import scala.io.Source
import scala.collection.immutable.{ TreeMap, TreeSet }
import scala.collection.mutable.{ HashMap, MultiMap, Set => MSet }

import java.lang.String

class ActiveSessionsDataExtractor(val runOn: String) extends Logging {
  val formattedRunOn = (new StringBuilder(runOn)).insert(12, ":").insert(10, ":").insert(8, " ").insert(6, "-").insert(4, "-").toString

  def getResults: List[(String, List[(String, Double)])] = {

    var executionStartDates: TreeSet[String] = TreeSet.empty
    // String: scenarioName, first MultiMap: active sessions, second MultiMap: dead sessions
    var scenariosToSessions: Map[String, (MultiMap[String, String], MultiMap[String, String])] = Map.empty

    logger.info("[Stats] reading from file: {}/{}", runOn, GATLING_SIMULATION_LOG_FILE)

    // Going through the specified log file
    for (line <- Source.fromFile(GATLING_RESULTS_FOLDER + "/" + runOn + "/" + GATLING_SIMULATION_LOG_FILE, "utf-8").getLines) {
      // Split each line by tabulation (As we get data from a TSV file)
      line.split("\t") match {
        // If we have a well formated result
        case Array(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage) => {

          val sessions = scenariosToSessions.get(scenarioName).getOrElse {
            val s = (new HashMap[String, MSet[String]] with MultiMap[String, String], new HashMap[String, MSet[String]] with MultiMap[String, String])
            scenariosToSessions += (scenarioName -> s)
            s
          }

          // Insert a value to keep the value in the set even if there's no active session
          scenariosToSessions.map { scenarioAndSessions =>
            scenarioAndSessions._2._1.addBinding(executionStartDate, "0")
          }

          // Depending on the type of action, we add the session to the good map
          if (actionName == "End of scenario") {
            sessions._2.addBinding(executionStartDate, userId)
          } else {
            sessions._1.addBinding(executionStartDate, userId)
          }

          // Storing date in a sorted set (to be used later for global active sessions)
          executionStartDates = executionStartDates + executionStartDate
        }
        // Else, if the resulting data is not well formated print an error message
        case _ => sys.error("Input file not well formatted")
      }
    }

    // Adding the missing active/dead sessions to the different executionStartDate
    var lastDate: String = formattedRunOn

    scenariosToSessions.foreach { scenarioAndSessions =>
      val (scName, (activeSessions, deadSessions)) = scenarioAndSessions
      executionStartDates.map { startDate =>
        if (startDate != formattedRunOn) {
          activeSessions.get(lastDate).map {
            set =>
              set.map {
                userId =>
                  activeSessions.addBinding(startDate, userId)
              }
              deadSessions.get(startDate).map { set =>
                set.map {
                  userId =>
                    activeSessions.removeBinding(startDate, userId)
                }
              }
          }
        }
        lastDate = startDate
      }
    }

    // String: scenarioName, String: Date, Double: Number Of ActiveSessions
    var results: List[(String, List[(String, Double)])] = Nil

    // Creating results for each scenario
    scenariosToSessions.map { scenarioAndSessions =>
      var scenarioResults: List[(String, Double)] = Nil
      var lastSize = 0D
      executionStartDates.map { startDate =>
        val sizeAsDouble: Double = scenariosToSessions.get(scenarioAndSessions._1).get._1.get(startDate).map(set => (set.size - 1).toDouble).getOrElse(lastSize)
        scenarioResults = (startDate, sizeAsDouble) :: scenarioResults
      }
      results = (scenarioAndSessions._1, scenarioResults) :: results
    }

    var globalResults: Map[String, Double] = Map.empty
    results.foreach { scenarioAndResults =>
      scenarioAndResults._2.map { result =>
        globalResults = globalResults + (result._1 -> (globalResults.get(result._1).getOrElse(0D).asInstanceOf[Double] + result._2))
      }
    }

    results = ("All Scenarios", globalResults.toList) :: results
    results.reverse
  }
}