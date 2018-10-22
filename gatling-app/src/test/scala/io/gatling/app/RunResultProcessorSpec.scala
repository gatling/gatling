/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.app

import io.gatling.BaseSpec
import io.gatling.charts.report.GatlingReportsGenerator
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingPropertiesBuilder

class MyReportsGenerator(implicit configuration: GatlingConfiguration) extends GatlingReportsGenerator

class RunResultProcessorSpec extends BaseSpec {

  private implicit val configuration =
    GatlingConfiguration.loadForTest(
      new GatlingPropertiesBuilder().resultsDirectory("src/test/resources").build ++
        Map[String, Any](RunResultProcessor.GATLING_REPORTS_CLASS -> classOf[MyReportsGenerator].getName)
    )

  it should "successfully validate gatling reports generator class" in {
    RunResultProcessor.validateReportsGenerator(configuration)
  }

  it should "successfully instantiate gatling reports generator using configured class" in {
    RunResultProcessor.getReportsGenerator(configuration)
  }

  it should "successfully instantiate gatling reports generator without configured class" in {
    RunResultProcessor
      .getReportsGenerator(
        GatlingConfiguration
          .loadForTest(new GatlingPropertiesBuilder().resultsDirectory("src/test/resources").build)
      )
  }
}
