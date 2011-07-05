package com.excilys.ebi.gatling.statistics

import org.fusesource.scalate._

import java.io.FileWriter

object Main {
  def main(args: Array[String]) {

    val dates = List("04:29:35", "04:29:38", "04:29:40", "04:29:41", "04:29:43",
      "04:29:44", "04:29:46", "04:29:47", "04:29:49", "04:29:50", "04:29:51")
    val values = List("5", "5", "5", "5", "5", "5", "5", "5", "5", "5", "0")
    val title = "Gatling Rocks !"

    val engine = new TemplateEngine
    engine.bindings = List(Binding("title", "String"), Binding("dates", "List[String]"), Binding("values", "List[String]"))
    val output = engine.layout("templates/layout.ssp", Map("title" -> title, "dates" -> dates, "values" -> values))
    val fw = new FileWriter("graph.html")
    fw.write(output)
    fw.close
  }
}