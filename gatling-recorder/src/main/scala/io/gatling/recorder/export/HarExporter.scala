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

import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.har.HarReader
import io.gatling.core.validation._
import io.gatling.recorder.model.SimulationModel

class HarExporter(harFilePath: String)(implicit model: SimulationModel, config: RecorderConfiguration) extends Exporter {

  def exportHar(implicit config: RecorderConfiguration): Validation[Unit] =
    try {
      val model = HarReader(harFilePath)
      if (model.isEmpty) {
        "the selected file doesn't contain any valid HTTP requests".failure
      } else {
        export
      }
    } catch {
      case e: Exception =>
        logger.error("Error while processing HAR file", e)
        e.getMessage.failure
    }

}