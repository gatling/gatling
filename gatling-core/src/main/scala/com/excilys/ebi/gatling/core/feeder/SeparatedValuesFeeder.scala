package com.excilys.ebi.gatling.core.feeder

import scala.io.Source
import scala.collection.immutable.HashMap
import scala.collection.mutable.Queue
import com.excilys.ebi.gatling.core.config.GatlingConfig._

class SeparatedValuesFeeder(fileName: String, mappings: List[String], val separator: String, val extension: String) extends Feeder(fileName, mappings) {

  val encoding = config.getString("gatling.feeders.encoding", "utf-8")

  var seeds: Queue[Map[String, String]] = Queue()

  logger.debug("Feeder Encoding : {}", encoding)

  for (line <- Source.fromFile("user-files/seeds/" + fileName + extension, encoding).getLines) {
    var lineMap = new HashMap[String, String]

    for (mapping <- mappings zip line.split(separator).toList)
      lineMap = lineMap + mapping

    seeds = seeds += lineMap
  }

  logger.debug("Feeder Seeds Loaded")

  def next: Map[String, String] = seeds.dequeue
}