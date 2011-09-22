package com.excilys.ebi.gatling.core.feeder

import scala.io.Source
import scala.collection.immutable.HashMap
import scala.collection.mutable.Queue

import com.excilys.ebi.gatling.core.config.GatlingConfig._
import com.excilys.ebi.gatling.core.util.PathHelper._

class SeparatedValuesFeeder(fileName: String, mappings: List[String], val separator: String, val extension: String) extends Feeder(fileName, mappings) {

  var seeds: Queue[Map[String, String]] = Queue()

  for (line <- Source.fromFile(GATLING_SEEDS_FOLDER + "/" + fileName + extension, CONFIG_GATLING_FEEDER_ENCODING).getLines) {
    var lineMap = new HashMap[String, String]

    for (mapping <- mappings zip line.split(separator).toList)
      lineMap = lineMap + mapping

    seeds += lineMap
  }

  logger.debug("Feeder Seeds Loaded")

  def next: Map[String, String] = seeds.dequeue
}