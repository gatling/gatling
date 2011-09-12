package com.excilys.ebi.gatling.core.feeder

import scala.io.Source
import scala.collection.immutable.HashMap
import scala.collection.mutable.Queue

class SeparatedValuesFeeder(fileName: String, mappings: List[String], val separator: String, val extension: String) extends Feeder(fileName, mappings) {

  var seeds: Queue[Map[String, String]] = Queue()

  // FIXME: utf-8 should be configurable
  for (line <- Source.fromFile("user-files/seeds/" + fileName + extension, "utf-8").getLines) {
    var lineMap = new HashMap[String, String]

    for (mapping <- mappings zip line.split(separator).toList)
      lineMap = lineMap + mapping

    seeds = seeds += lineMap
  }

  logger.debug("Feeder Seeds Loaded")

  def next: Map[String, String] = seeds.dequeue
}