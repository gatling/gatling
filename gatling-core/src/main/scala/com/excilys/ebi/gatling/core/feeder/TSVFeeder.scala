package com.excilys.ebi.gatling.core.feeder

import scala.io.Source
import scala.collection.immutable.HashMap

class TSVFeeder(filePath: String, mappings: List[String]) extends Feeder(filePath, mappings) {

  var currentUser = 0
  var seeds: List[Map[String, String]] = List()

  for (line <- Source.fromFile("user-seeds/" + filePath + ".tsv", "utf-8").getLines) {
    var lineMap = new HashMap[String, String]

    for (mapping <- mappings zip line.split("\t").toList)
      lineMap = lineMap + mapping

    seeds = lineMap :: seeds
  }

  logger.debug("Feeder Seeds: {}", seeds)

  def get(key: String)(index: Int): String = {
    seeds(index).get(key).getOrElse(throw new Exception("Problem with feeder"))
  }

  def nextIndex: Int = {
    val index = currentUser
    if (currentUser < seeds.size - 1)
      currentUser += 1
    else
      currentUser = 0
    index
  }
}