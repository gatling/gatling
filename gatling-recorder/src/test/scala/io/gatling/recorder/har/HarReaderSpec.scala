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
package io.gatling.recorder.har

import scala.concurrent.duration.DurationInt

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import io.gatling.recorder.config.ConfigurationConstants.FETCH_HTML_RESOURCES
import io.gatling.recorder.config.RecorderConfiguration.fakeConfig
import io.gatling.recorder.scenario.{ PauseElement, RequestElement }

@RunWith(classOf[JUnitRunner])
class HarReaderSpec extends Specification {

  def resourceAsStream(p: String) = getClass.getClassLoader.getResourceAsStream(p)

  "HarReader" should {

    val configWithResourcesFiltering = fakeConfig(Map(FETCH_HTML_RESOURCES -> true))

    // By default, we assume that we doHeren't want to filter out the HTML resources
    implicit val config = fakeConfig(Map(FETCH_HTML_RESOURCES -> false))

    "work with empty JSON" in {
      HarReader(resourceAsStream("har/empty.har")) must beEmpty
    }

    val scn = HarReader(resourceAsStream("har/www.kernel.org.har"))
    val elts = scn.elements
    val pauseElts = elts.collect { case PauseElement(duration) => duration }

    "return the correct number of Pause elements" in {
      pauseElts.size must beLessThan(elts.size / 2)
    }

    "return an appropriate pause duration" in {
      val pauseDuration = pauseElts.reduce(_ + _)

      // The total duration of the HAR record is of 6454ms
      (pauseDuration must beLessThanOrEqualTo(88389 milliseconds)) and
        (pauseDuration must beGreaterThan(80000 milliseconds))
    }

    "return the appropriate request elements" in {
      val (googleFontUris, uris) = elts
        .collect { case RequestElement(uri, _, _, _, _, _) => uri }
        .partition(_.contains("google"))

      (uris must contain(startingWith("https://www.kernel.org")).forall) and
        (uris.size must beEqualTo(41)) and
        (googleFontUris.size must beEqualTo(16))
    }

    "have the approriate first requests" in {
      // The first element can't be a pause.
      (elts.head must beAnInstanceOf[RequestElement]) and
        (elts.head.asInstanceOf[RequestElement].uri must beEqualTo("https://www.kernel.org/")) and
        (elts(1) must beAnInstanceOf[RequestElement]) and
        (elts(1).asInstanceOf[RequestElement].uri must beEqualTo("https://www.kernel.org/theme/css/main.css"))
    }

    "have the headers correctly set" in {
      val el0 = elts.head.asInstanceOf[RequestElement]
      val el1 = elts(1).asInstanceOf[RequestElement]

      (el0.headers must beEmpty) and
        (el1.headers must not beEmpty) and
        (el1.headers must haveKeys("User-Agent", "Host", "Accept-Encoding", "Accept-Language"))
    }

    "have requests with valid headers" in {
      // Extra headers can be added by Chrome
      val headerNames = elts.iterator.collect { case RequestElement(_, _, headers, _, _, _) => headers.keys }.flatten.toSet
      headerNames must not containPattern (":.*")
    }

    "have the embedded HTML resources filtered out" in {
      val scn2 = HarReader(resourceAsStream("har/www.kernel.org.har"))(configWithResourcesFiltering)
      val elts2 = scn2.elements
      elts2.size must beLessThan(elts.size) and
        (elts2 must contain("https://www.kernel.org/theme/css/main.css") not)
    }

    "deal correctly with file having a websockets record" in {
      val scn = HarReader(resourceAsStream("har/play-chat.har"))(configWithResourcesFiltering)
      val requests = scn.elements.collect { case r @ RequestElement(_, _, _, _, _, _) => r.uri }

      (scn.elements must have size (3)) and
        (requests must beEqualTo(List("http://localhost:9000/room", "http://localhost:9000/room?username=robert")))
    }
  }

  // Deactivate Specs2 implicit to be able to use the ones provided in scala.concurrent.duration
  override def intToRichLong(v: Int) = super.intToRichLong(v)
}