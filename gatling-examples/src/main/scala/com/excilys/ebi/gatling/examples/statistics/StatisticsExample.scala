package com.excilys.ebi.gatling.examples.statistics

import com.excilys.ebi.gatling.statistics.DataWriter

object StatisticsExample {
  def run(runOn: String) = {
    new DataWriter.writeUsersStats(runOn)
  }
}