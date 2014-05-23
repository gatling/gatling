/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.recorder.export

import java.io.{ FileOutputStream, IOException }
import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.reflect.io.Path.string2path
import scala.tools.nsc.io.{ Directory, File }
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.core.validation._
import io.gatling.core.util.IO
import io.gatling.http.HeaderNames
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.har.HarReader
import io.gatling.recorder.model.SimulationModel
import io.gatling.recorder.export.template._

object Exporter {

  def simulationExists(implicit config: RecorderConfiguration) = {
    val p: scala.reflect.io.Path = getFilePath("${config.core.className}")

    p.exists
  }

  def getFilePath(fileName: String)(implicit config: RecorderConfiguration) = {
      def getSimulationFileName: String = fileName + ".scala"
      def getOutputFolder = {
        val path = config.core.outputFolder + File.separator + config.core.pkg.replace(".", File.separator)
        getFolder(path)
      }

    getOutputFolder / getSimulationFileName
  }

  def getFolder(folderPath: String) = Directory(folderPath).createDirectory()

}

class Exporter(implicit config: RecorderConfiguration) extends StrictLogging with IO {

  var model: SimulationModel = _

  /**
   * replacement for saveScenario
   *
   * http://martinfowler.com/bliki/PageObject.html
   *
   * https://code.google.com/p/selenium/wiki/PageObjects
   *
   * https://github.com/wiredrive/wtframework/wiki/PageFactory
   *
   * https://github.com/wiredrive/wtframework/wiki/WTF-PageObject-Utility-Chrome-Extension
   *
   * https://github.com/excilys/gatling/wiki/Advanced-Usage
   *
   * https://github.com/calabash/calabash-ios/wiki/04-Touch-recording-and-playback
   */
  def export(model: SimulationModel): Validation[Unit] = {

    try {
      this.model = model
      require(!model.isEmpty)

      // simulation
      renderAndSave(SimulationTemplate.render)

      // scenarios
      renderAndSave(ScenarioTemplate.render)

      // interactions
      renderAndSave(NavigationTemplate.render)

      // requests
      renderAndSave(RequestTemplate.render)

      // protocol
      renderAndSave(ProtocolTemplate.render)

      // request bodies	
      renderAndSave(RequestBodyTemplate.render).success

    } catch {
      case e: Exception =>
        logger.error("Error while exporting", e)
        e.getMessage.failure
    }
  }

  //alexb
  /**
   * (String,Any) ==>> (filename , rendered output)
   */
  def renderAndSave(render: SimulationModel => Seq[(String, Any)]) {

      def save(fName: String, output: Any): Unit = {

        val filePath = Exporter.getFilePath(fName)

        withCloseable(new FileOutputStream(File(filePath).jfile)) {
          output match {
            case string: String => {
              _.write(string.getBytes(config.core.encoding))
            }
            case bytes: Byte => {
              _.write(bytes)
            }
          }
        }

      }

    render(model).foreach(e => {
      val (fName, output) = e
      save(fName, output)
    })
  }

}