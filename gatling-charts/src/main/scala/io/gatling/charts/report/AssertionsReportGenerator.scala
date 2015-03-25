package io.gatling.charts.report

import io.gatling.charts.component.ComponentLibrary
import io.gatling.charts.config.ChartsFiles._
import io.gatling.charts.template.{ AssertionsJsonTemplate, AssertionsJUnitTemplate }
import io.gatling.core.config.GatlingConfiguration

private[charts] class AssertionsReportGenerator(reportsGenerationInputs: ReportsGenerationInputs, componentLibrary: ComponentLibrary)(implicit configuration: GatlingConfiguration) {

  import reportsGenerationInputs._

  def generate(): Unit = {
    new TemplateWriter(assertionsJUnitFile(reportFolderName)).writeToFile(new AssertionsJUnitTemplate(dataReader.runMessage, assertionResults).getOutput)
    new TemplateWriter(assertionsJsonFile(reportFolderName)).writeToFile(new AssertionsJsonTemplate(assertionResults).getOutput)
  }
}
