package com.excilys.ebi.gatling.statistics.template

class Series(val name: String, val data: List[(String, Double)]) {
  override def toString = {
    "name: '" + name + "', data: " + data.map { e => "[" + e._1 + ", " + e._2 + "]" }.mkString("[ ", ",", " ]")
  }
}