package com.excilys.ebi.gatling.examples

import com.excilys.ebi.gatling.examples.http.HttpExample
import com.excilys.ebi.gatling.examples.statistics.StatisticsExample

object App {
  def main(args: Array[String]) {
    args(0) match {
      case "http" => HttpExample.run
      case "stats" => StatisticsExample.run
      case _ =>
    }
  }
}
