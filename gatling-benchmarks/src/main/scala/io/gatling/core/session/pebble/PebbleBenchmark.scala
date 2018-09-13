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

package io.gatling.core.session.pebble

import scala.util.Random

import io.gatling.Utils._
import io.gatling.commons.validation.Validation
import io.gatling.core.body.PebbleStringBody
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session

import org.openjdk.jmh.annotations.Benchmark

object PebbleBenchmark {
  private implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private val charset = configuration.core.charset

  private val session: Session = new Session("Scenario", 0).set("id", 3)
  private val SinglePlaceHolderPebbleBody = PebbleStringBody(resourceAsString("sample-peeble.json", charset))

  private val session2: Session = new Session("Scenario", 0).setAll("id" -> 3, "friends" -> Seq.fill(20)(Random.nextInt))
  private val LoopPebbleBody = PebbleStringBody(resourceAsString("sample-peeble2.json", charset))
}

class PebbleBenchmark {
  import PebbleBenchmark._

  @Benchmark
  def testSinglePlaceHolder(): Validation[String] =
    SinglePlaceHolderPebbleBody.apply(session)

  @Benchmark
  def testLoop(): Validation[String] =
    LoopPebbleBody.apply(session2)
}
