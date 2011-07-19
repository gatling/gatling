package com.excilys.ebi.gatling.statistics;

import com.excilys.ebi.gatling.core.log.Logging
import org.fusesource.scalate._

import java.io.FileWriter
import java.io.File

class GlobalRequestsDataPresenter extends Logging {
  def generateGraphFor(runOn: String) {
    val title = "Requests"

    var dates: List[String] = Nil
    var globalValues: List[Int] = Nil
    var okValues: List[Int] = Nil
    var koValues: List[Int] = Nil

    new GlobalRequestsDataExtractor(runOn).getResults foreach {
      case (date, (numberOfRequests, numberOfSuccesses, numberOfFailures)) =>
        dates = date.substring(11) :: dates
        globalValues = numberOfRequests :: globalValues
        okValues = numberOfSuccesses :: okValues
        koValues = numberOfFailures :: koValues
    }

    logger.debug("Dates: {}\nValues: {}", dates, (globalValues, okValues, koValues))

    val engine = new TemplateEngine
    engine.bindings = List(
      Binding("title", "String"),
      Binding("dates", "List[String]"),
      Binding("globalValues", "List[Int]"),
      Binding("okValues", "List[Int]"),
      Binding("koValues", "List[Int]"),
      Binding("runOn", "String"))

    val output = engine.layout("templates/layout_requests.ssp",
      Map("title" -> title,
        "dates" -> dates.reverse,
        "globalValues" -> globalValues.reverse,
        "okValues" -> okValues.reverse,
        "koValues" -> koValues.reverse,
        "runOn" -> runOn))

    val dir = new File(runOn)
    dir.mkdir
    val file = new File(dir, "requests.html")
    val fw = new FileWriter(file)
    fw.write(output)
    fw.close
  }
}
