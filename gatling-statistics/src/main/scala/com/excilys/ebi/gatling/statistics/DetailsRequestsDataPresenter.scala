package com.excilys.ebi.gatling.statistics

import com.excilys.ebi.gatling.core.log.Logging
import org.fusesource.scalate._

import java.io.File
import java.io.FileWriter

class DetailsRequestsDataPresenter extends Logging {
  def generateGraphFor(runOn: String) = {
    new DetailsRequestsDataExtractor(runOn).getResults.foreach {
      case (requestName, result) =>

        val title = "Details of '" + requestName + "'"

        var dates: List[String] = Nil
        var values: List[Int] = Nil

        result.values foreach {
          case (date, responseTime) =>
            dates = date.substring(11) :: dates
            values = responseTime :: values
        }

        logger.debug("Dates: {}\nValues: {}", dates, values)

        val engine = new TemplateEngine
        engine.bindings = List(
          Binding("title", "String"),
          Binding("dates", "List[String]"),
          Binding("values", "List[Int]"),
          Binding("runOn", "String"),
          Binding("requestName", "String"),
          Binding("result", "com.excilys.ebi.gatling.statistics.DetailsRequestsDataResult"))

        val output = engine.layout("templates/layout_details_requests.ssp",
          Map("title" -> title,
            "dates" -> dates.reverse,
            "values" -> values.reverse,
            "runOn" -> runOn,
            "requestName" -> requestName,
            "result" -> result))

        val dir = new File(runOn)
        dir.mkdir
        val file = new File(dir, requestNameToFileName(requestName) + ".html")
        val fw = new FileWriter(file)
        fw.write(output)
        fw.close
    }
  }

  private def requestNameToFileName(requestName: String): String = requestName.replace("-", "_").replace(" ", "_").toLowerCase
}