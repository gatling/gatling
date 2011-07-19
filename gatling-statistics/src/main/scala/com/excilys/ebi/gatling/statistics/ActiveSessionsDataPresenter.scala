package com.excilys.ebi.gatling.statistics;

import com.excilys.ebi.gatling.core.log.Logging
import org.fusesource.scalate._

import java.io.FileWriter
import java.io.File

class ActiveSessionsDataPresenter extends Logging {
  def generateGraphFor(runOn: String) = {
    val title = "Active Sessions"

    var dates: List[String] = Nil
    var values: List[Int] = Nil

    new ActiveSessionsDataExtractor(runOn).getResults foreach {
      case (date, numberOfActiveSessions) =>
        dates = date.substring(11) :: dates
        values = numberOfActiveSessions :: values
    }

    logger.debug("Dates: {}\nValues: {}", dates, values)

    val engine = new TemplateEngine
    engine.bindings = List(
      Binding("title", "String"),
      Binding("dates", "List[String]"),
      Binding("values", "List[Int]"),
      Binding("runOn", "String"))

    val output = engine.layout("templates/layout_active_sessions.ssp",
      Map("title" -> title,
        "dates" -> dates.reverse,
        "values" -> values.reverse,
        "runOn" -> runOn))

    val dir = new File(runOn)
    dir.mkdir
    val file = new File(dir, "active_sessions.html")
    val fw = new FileWriter(file)
    fw.write(output)
    fw.close
  }
}
