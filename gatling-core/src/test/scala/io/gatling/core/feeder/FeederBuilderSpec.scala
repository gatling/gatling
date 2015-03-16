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
package io.gatling.core.feeder

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.validation.Failure
import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.immutable

class FeederBuilderSpec extends FlatSpec with Matchers with FeederSupport {

  implicit val configuration = GatlingConfiguration.loadForTest()

  "RecordSeqFeederBuilder" should "be able to use all the strategies" in {
    val builder = RecordSeqFeederBuilder(IndexedSeq())
    builder.queue.strategy shouldBe Queue
    builder.random.strategy shouldBe Random
    builder.circular.strategy shouldBe Circular
  }

  "RecordSeqFeederBuilder" should "throw an exception when provided with bad resource" in {
    an[IllegalArgumentException] should be thrownBy
      feederBuilder(Failure(""))(SeparatedValuesParser.parse(_, SeparatedValuesParser.CommaSeparator, '\'', rawSplit = false))
  }

  "RecordSeqFeederBuilder" should "build a Feeder behaving accordingly to each strategy" in {
    //Queue
    val queuedFeeder = RecordSeqFeederBuilder(IndexedSeq(Map("1" -> "Test"), Map("2" -> "Test"))).queue.build
    queuedFeeder.toArray shouldBe Array(Map("1" -> "Test"), Map("2" -> "Test"))

    //Random
    val fiftyTimes = 1 to 50
    val orderedMaps =
      fiftyTimes.foldLeft(IndexedSeq.empty[Record[String]]) { (acc, id) => Map(id.toString -> "Test") +: acc }

    val testsOutcome: immutable.IndexedSeq[Boolean] =
      (1 to 3).map { _ =>
        val randomFeeder = RecordSeqFeederBuilder(orderedMaps).random.build
        randomFeeder.hasNext shouldBe true
        val retrievedMaps = fiftyTimes.map(_ => randomFeeder.next())
        retrievedMaps != orderedMaps
      }

    if (!testsOutcome.reduce(_ || _)) fail("Random feeder did not return a random order even once out of three attempts")

    //Circular
    val circularFeeder = RecordSeqFeederBuilder(IndexedSeq(Map("1" -> "Test"), Map("2" -> "Test"))).circular.build
    circularFeeder.next()
    circularFeeder.next()
    circularFeeder.next() shouldBe Map("1" -> "Test")

  }

  "RecordSeqFeederBuilder" should "be able to have a record converted" in {
    val queuedFeeder = RecordSeqFeederBuilder(IndexedSeq(Map("1" -> "Test"), Map("2" -> "Test")))
    val convertedValue: Option[Any] = queuedFeeder.convert {
      case ("1", attr) => attr.concat("s are boring !")
    }.records(0).get("1")

    convertedValue.fold(fail("Could not find key"))(_ shouldBe "Tests are boring !")

    val cantConvert: Option[Any] = queuedFeeder.convert {
      case ("Can't find because don't exist", shouldKeepAsIs) => shouldKeepAsIs.concat("s are boring !")
    }.records(0).get("1")

    cantConvert.fold(fail("Could not find key"))(_ shouldBe "Test")
  }

  "FeederBuilder" should "have working implicit conversions" in {
    IndexedSeq(Map("1" -> "Test")).build shouldBe a[Feeder[_]]
    val convertedObj = Array(Map("1" -> "Test")).build
    convertedObj shouldBe a[Feeder[_]]
    convertedObj.build shouldBe a[Feeder[_]]
  }

}
