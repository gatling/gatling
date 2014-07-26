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
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.junit.JUnitRunner

import io.gatling.core.util.IO.withCloseable
import io.gatling.recorder.config.ConfigKeys.http.InferHtmlResources
import io.gatling.recorder.config.RecorderConfiguration.fakeConfig
import io.gatling.recorder.scenario.{ PauseElement, RequestElement }

@RunWith(classOf[JUnitRunner])
class HarReaderSpec extends FlatSpec with Matchers {

  def resourceAsStream(p: String) = getClass.getClassLoader.getResourceAsStream(p)

  val configWithResourcesFiltering = fakeConfig(Map(InferHtmlResources -> true))

  // By default, we assume that we don't want to filter out the HTML resources
  implicit val config = fakeConfig(Map(InferHtmlResources -> false))

  "HarReader" should "work with empty JSON" in {
    withCloseable(resourceAsStream("har/empty.har"))(HarReader(_) shouldBe empty)
  }

  val scn = withCloseable(resourceAsStream("har/www.kernel.org.har"))(HarReader(_))

  val elts = scn.elements
  val pauseElts = elts.collect { case PauseElement(duration) => duration }

  it should "return the correct number of Pause elements" in {
    pauseElts.size shouldBe <(elts.size / 2)
  }

  it should "return an appropriate pause duration" in {
    val pauseDuration = pauseElts.reduce(_ + _)

    // The total duration of the HAR record is of 6454ms
    pauseDuration shouldBe <=(88389 milliseconds)
    pauseDuration shouldBe >(80000 milliseconds)
  }

  it should "return the appropriate request elements" in {
    val (googleFontUris, uris) = elts
      .collect { case req: RequestElement => req.uri }
      .partition(_.contains("google"))

    all(uris) should startWith("https://www.kernel.org")
    uris.size shouldBe 41
    googleFontUris.size shouldBe 16
  }

  it should "have the approriate first requests" in {
    // The first element can't be a pause.
    elts.head shouldBe a[RequestElement]
    elts.head.asInstanceOf[RequestElement].uri shouldBe "https://www.kernel.org/"
    elts(1) shouldBe a[RequestElement]
    elts(1).asInstanceOf[RequestElement].uri shouldBe "https://www.kernel.org/theme/css/main.css"
  }

  it should "have the headers correctly set" in {
    val el0 = elts.head.asInstanceOf[RequestElement]
    val el1 = elts(1).asInstanceOf[RequestElement]

    val a = el0.headers shouldBe empty
    el1.headers should not be empty
    for {
      header <- List("User-Agent", "Host", "Accept-Encoding", "Accept-Language")
    } el1.headers should contain key header
  }

  it should "have requests with valid headers" in {
    // Extra headers can be added by Chrome
    val headerNames = elts.iterator.collect { case req: RequestElement => req.headers.keys }.flatten.toSet
    all(headerNames) should not include regex(":.*")
  }

  it should "have the embedded HTML resources filtered out" in {
    val scn2 = HarReader(resourceAsStream("har/www.kernel.org.har"))(configWithResourcesFiltering)
    val elts2 = scn2.elements
    elts2.size shouldBe <(elts.size)
    elts2 should not contain "https://www.kernel.org/theme/css/main.css"
  }

  it should "deal correctly with file having a websockets record" in {
    val scn = HarReader(resourceAsStream("har/play-chat.har"))(configWithResourcesFiltering)
    val requests = scn.elements.collect { case req: RequestElement => req.uri }

    scn.elements should have size 3
    requests shouldBe List("http://localhost:9000/room", "http://localhost:9000/room?username=robert")
  }

  it should "deal correctly with HTTP CONNECT requests" in {
    val scn = HarReader(resourceAsStream("har/charles_https.har"))

    scn.elements shouldBe empty
  }

  it should "deal correctly with HTTP requests having a status=0" in {
    val scn = HarReader(resourceAsStream("har/null_status.har"))
    val requests = scn.elements.collect { case req: RequestElement => req }
    val statuses = requests.map(_.statusCode)

    requests should have size 3
    statuses should not contain 0
  }

}
