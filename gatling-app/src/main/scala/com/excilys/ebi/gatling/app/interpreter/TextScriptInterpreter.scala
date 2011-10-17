package com.excilys.ebi.gatling.app.interpreter

import com.excilys.ebi.gatling.core.util.PathHelper._
import com.excilys.ebi.gatling.core.util.FileHelper._
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.IMain
import scala.io.Source
import scala.util.matching.Regex
import org.joda.time.DateTime

/**
 * Simple Class used to get a value from the interpreter
 */
class DateHolder(var value: DateTime)

class TextScriptInterpreter extends Interpreter {

	def run(fileName: String, startDate: DateTime) = {
		// Sets the interpreter to use the classpath of the java command
		val settings = new Settings
		settings.usejavacp.value = true

		val n = new IMain(settings)

		// This is the file header, with all needed imports and declarations
		val fileHeader = """
    import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder._
    import com.excilys.ebi.gatling.core.feeder._
    import com.excilys.ebi.gatling.core.context._
    import com.excilys.ebi.gatling.core.util.StringHelper._
    import com.excilys.ebi.gatling.core.runner.Runner._
    import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder._
    import com.excilys.ebi.gatling.core.scenario.builder.ChainBuilder._
    import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder._
    import com.excilys.ebi.gatling.http.processor.capture.builder.HttpRegExpCaptureBuilder._
    import com.excilys.ebi.gatling.http.processor.capture.builder.HttpXPathCaptureBuilder._
    import com.excilys.ebi.gatling.http.processor.capture.builder.HttpHeaderCaptureBuilder._
    import com.excilys.ebi.gatling.http.processor.check.builder.HttpXPathCheckBuilder._
    import com.excilys.ebi.gatling.http.processor.check.builder.HttpRegExpCheckBuilder._
    import com.excilys.ebi.gatling.http.processor.check.builder.HttpStatusCheckBuilder._
    import com.excilys.ebi.gatling.http.processor.check.builder.HttpHeaderCheckBuilder._
    import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
    import java.util.concurrent.TimeUnit
    import org.joda.time.DateTime
    
    def runSimulations = runSim(startDate.value)_
    """

		// Contains the contents of the simulation file
		val initialFileBodyContent = Source.fromFile(GATLING_SCENARIOS_FOLDER + "/" + fileName).mkString

		// Includes contents of included files into the simulation file 
		val toBeFound = new Regex("""include\("(.*)"\)""")
		val newFileBodyContent = toBeFound replaceAllIn (initialFileBodyContent, result => {
			val partialName = result.group(1)
			var path =
				if (partialName.startsWith("_")) {
					partialName
				} else {
					fileName.substring(0, fileName.length() - 4) + "/" + partialName
				}
			Source.fromFile(GATLING_SCENARIOS_FOLDER + "/" + path + TXT_EXTENSION).mkString + "\n\n"
		})

		// Complete script
		val fileContent = fileHeader + newFileBodyContent
		logger.debug(fileContent)

		n.bind("startDate", new DateHolder(startDate))
		n.interpret(fileContent) // This is where the simulation starts
		n.close()
	}
}