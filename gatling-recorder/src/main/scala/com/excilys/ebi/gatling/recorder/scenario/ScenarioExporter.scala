/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.recorder.scenario;

import java.io.FileWriter
import java.io.IOException
import java.util.Date
import scala.Option.option2Iterable
import scala.math.min
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.Directory
import scala.tools.nsc.io.File
import org.apache.commons.io.IOUtils.closeQuietly
import org.fusesource.scalate.TemplateEngine
import com.excilys.ebi.gatling.core.config.GatlingFiles.GATLING_REQUEST_BODIES
import com.excilys.ebi.gatling.core.util.IOHelper.use
import com.excilys.ebi.gatling.recorder.config.Configuration.configuration
import com.excilys.ebi.gatling.recorder.config.Configuration
import com.excilys.ebi.gatling.recorder.controller.RecorderController
import grizzled.slf4j.Logging
import java.text.Format
import java.text.SimpleDateFormat
import com.excilys.ebi.gatling.http.ahc.GatlingAsyncHandlerActor.REDIRECT_STATUS_CODES

object ScenarioExporter extends Logging {
	val DATE_FORMATTER: Format = new SimpleDateFormat("yyyyMMddHHmmss")
	
	private val EVENTS_GROUPING = 100
	
	val TPL_ENGINE = new TemplateEngine
	TPL_ENGINE.allowReload = false
	TPL_ENGINE.escapeMarkup = false

	def saveScenario(startDate: Date, scenarioElements: List[ScenarioElement]) = {

		val baseUrl = getBaseUrl(scenarioElements)
		
		val protocolConfigElement = new ProtocolConfigElement(baseUrl, configuration.proxy, configuration.followRedirect)

		val simulationClass = 
			if(configuration.simulationClassName != Configuration.DEFAULT_CLASS_NAME)
				configuration.simulationClassName
			else
				configuration.simulationClassName + DATE_FORMATTER.format(startDate)
		
		// If follow redirect, discard some recorded elements
		def getStatusCode(se: ScenarioElement) = {
			se match {
				case r: RequestElement => r.statusCode
				case _ => 0
			}
		}
		
		def filterRedirectsAndNonAuthorized(l: List[(ScenarioElement, Int)], e: ScenarioElement) = {
			if(l.isEmpty)
				e match {
					case r: RequestElement => List((r, r.statusCode))
					case x => List((x, 0))
				}
			else
				l.head match{
					case (lastRequestElement: RequestElement, lastStatusCode: Int) =>
						if(REDIRECT_STATUS_CODES.contains(lastStatusCode))
							e match{
								case r: RequestElement => (RequestElement(lastRequestElement, r.statusCode), r.statusCode) :: l.tail
								case _ => l
							}
						else
							(e, getStatusCode(e)) :: l
					case _ => (e, getStatusCode (e)) :: l
				}
		}

		val filteredElements =
			if(configuration.followRedirect)
				scenarioElements.foldLeft(List[(ScenarioElement,Int)]())(filterRedirectsAndNonAuthorized).map{
					case (element, statusCode) => element
				}.reverse
			else
				scenarioElements

		// Add simulationClass to request elements
		val elementsList = filteredElements.map{
			case e:RequestElement => RequestElement(e, simulationClass)
			case e => e
		}
				
		// Updates URLs that contain baseUrl, set ids on requests and dump request body if needed
		var i = 0
		elementsList.foreach{
			case e: RequestElement => {
				i = i + 1
				e.updateUrl(baseUrl).setId(i)
				e.requestBody.foreach { content =>
					dumpRequestBody(i, content, simulationClass)
				}
			}
			case _ =>
		}
		
		// Aggregate headers
		val headers = elementsList.map{
			case e: RequestElement => Some(
				(e.id, e.headers.filterNot(header => Array("Authorization", "Cookie", "Content-Length").contains(header._1)))
			)
			case _ => None
		}.flatten
				
		val (newScenarioElements, chains) = getChains(elementsList)
		
		val output = ScenarioExporter.TPL_ENGINE.layout("templates/simulation.ssp",
			Map("protocolConfig" -> protocolConfigElement,
				"headers" -> headers,
				"simulationClassName" -> simulationClass,
				"scenarioName" -> "Scenario Name",
				"packageName" -> configuration.simulationPackage,
				"chains" -> chains,
				"scenarioElements" -> newScenarioElements))

		use(new FileWriter(File(getOutputFolder / getScenarioFileName(startDate)).jfile)) { _.write(output) }
	}

	private def getBaseUrl(scenarioElements: List[ScenarioElement]): String = {
		val baseUrls = scenarioElements.map{ 
			case reqElm: RequestElement => Some(reqElm.baseUrl)
			case _ => None
		}.flatten.groupBy(url => url).map{case (url, urls) => (url, urls.size)}
		
		baseUrls.maxBy{ case (url, nbOfOccurrences) => nbOfOccurrences }._1
	}
	
	private def getChains(scenarioElements: List[ScenarioElement]) : (List[ScenarioElement], List[List[ScenarioElement]]) = {
		var chains: List[List[ScenarioElement]] = Nil
		var newScenarioElements: List[ScenarioElement] = Nil

		if (scenarioElements.size > ScenarioExporter.EVENTS_GROUPING) {
			val numberOfSubLists = scenarioElements.size / ScenarioExporter.EVENTS_GROUPING + 1
			// Creates the content of the chains
			for (i <- 0 until numberOfSubLists){
				chains = scenarioElements.slice(0 + ScenarioExporter.EVENTS_GROUPING * i, min(ScenarioExporter.EVENTS_GROUPING * (i + 1), scenarioElements.size - 1)) :: chains
			}
		} else {
			newScenarioElements = scenarioElements
		}
		
		(newScenarioElements, chains.reverse)
	}
	
	private def dumpRequestBody(idEvent: Int, content: String, simulationClass: String) {

		var fw: FileWriter = null
		try {
			fw = new FileWriter(File(getFolder(configuration.requestBodiesFolder) / simulationClass + "_request_" + idEvent + ".txt").jfile)
			fw.write(content)
		} catch {
			case e: IOException =>
				error("Error, while dumping request body... \n" + e.getStackTrace)
		} finally {
			closeQuietly(fw)
		}
	}

	private def getScenarioFileName(date: Date): String = {
		"Simulation" + DATE_FORMATTER.format(date) + ".scala"
	}

	def getOutputFolder = getFolder(configuration.outputFolder)

	private def getFolder(folderPath: String) = Directory(folderPath).createDirectory()
}