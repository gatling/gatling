/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import java.io.File

import scala.util.Random

import io.gatling.commons.util.Io
import io.gatling.commons.validation.Validation
import io.gatling.core.body.PebbleStringBody
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.loader.StringLoader
import org.openjdk.jmh.annotations.Benchmark

object PebbleBenchmark {

  private val session: Session = new Session("Scenario", 0).set("id", 3)
  private val Json: String = new String(Io.RichFile(new File("src/main/resources/sample-peeble.json")).toByteArray)
  private val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()
  private val Engine = new PebbleEngine.Builder().loader(new StringLoader).build
  private val template = Engine.getTemplate(Json)
  implicit val charset = configuration.core.charset
  private val pebbleStringBody: PebbleStringBody = new PebbleStringBody(template)

  private val seq: Seq[Int] = Seq.fill(500)(Random.nextInt)
  private val session2: Session = new Session("Scenario", 0).setAll("id" -> 3, "friends" -> seq)
  private val Json2: String = new String(Io.RichFile(new File("src/main/resources/sample-peeble2.json")).toByteArray)
  private val template2 = Engine.getTemplate(Json2)
  private val pebbleStringBody2: PebbleStringBody = new PebbleStringBody(template2)
}

class PebbleBenchmark {
  import PebbleBenchmark._

  @Benchmark
  def testBasic(): Validation[String] =
    pebbleStringBody.apply(session)

  @Benchmark
  def testAdvanced(): Validation[String] =
    pebbleStringBody2.apply(session2)
}
