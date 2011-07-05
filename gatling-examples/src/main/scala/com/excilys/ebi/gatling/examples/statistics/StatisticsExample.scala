package com.excilys.ebi.gatling.examples.statistics

import com.excilys.ebi.gatling.statistics.UsersStatsDataWriter

object StatisticsExample {

  def run(runOn: String) = {
    val dw = new UsersStatsDataWriter(runOn)
    dw.writeStats
  }
}