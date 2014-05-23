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
import io.gatling.recorder.model.ModelFixtures
import io.gatling.recorder.model.ModelFixtures.config
import io.gatling.recorder.model.RequestModel
import io.gatling.recorder.model.ExecModel
import io.gatling.recorder.model.PauseModel

@RunWith(classOf[JUnitRunner])
class HarReaderSpec extends Specification {

  def resourceAsStream(p: String) = getClass.getClassLoader.getResourceAsStream(p)

  "HarReader" should {

      def kernel_org_har = resourceAsStream("har/www.kernel.org.har")
      def null_status_har = resourceAsStream("har/null_status.har")
      def play_chat_har = resourceAsStream("har/play-chat.har")
      def empty_har = resourceAsStream("har/empty.har")
      def charles_https_har = resourceAsStream("har/charles_https.har")

    "be able to read in various har files" in {

      ModelFixtures.config = ModelFixtures.config_FetchHtmlResources_false

      HarReader(kernel_org_har) must not be None
      HarReader(null_status_har) must not be None
      HarReader(play_chat_har) must not be None

      HarReader(empty_har) must not be None
      HarReader(charles_https_har) must not be None
    }

    "work with empty JSON" in {

      ModelFixtures.config = ModelFixtures.config_FetchHtmlResources_false

      val model = HarReader(empty_har)
      model must beEmpty
    }

    "return the appropriate request elements" in {

      ModelFixtures.config = ModelFixtures.config_FetchHtmlResources_false

      val model = HarReader(kernel_org_har)
      val elts = model.getRequests

      val (googleFontUris, uris) = elts
        .collect { case RequestModel(uri, _, _, _, _, _, _) => uri }
        .partition(
          _.contains("google"))

      (uris must contain(startingWith("https://www.kernel.org")).forall) and
        (uris.size must beEqualTo(14)) and // 14 UNIQUE requests, 41 in total....
        (googleFontUris.size must beEqualTo(4)) // 4 UNIQUE, 16 in total
    }

    "have requests with valid headers" in {

      ModelFixtures.config = ModelFixtures.config_FetchHtmlResources_false

      val model = HarReader(kernel_org_har)
      val elts = model.getRequests

      // Extra headers can be added by Chrome
      val headerNames = elts.iterator.collect { case RequestModel(_, _, headers, _, _, _, _) => headers.keys }.flatten.toSet

      headerNames must not containPattern (":.*")
      headerNames must containPattern("accept-language")
      headerNames must containPattern("accept-encoding")
    }

    //TODO
    "have the embedded HTML resources filtered out" in pending {

      ModelFixtures.config = ModelFixtures.config_FetchHtmlResources_false

      val model = HarReader(kernel_org_har)
      val elts = model.getRequests

      val model2 = HarReader(kernel_org_har)(ModelFixtures.config_basic)
      val elts2 = model2.getRequests

      elts2.size must beLessThan(elts.size)
      (elts2 must contain("https://www.kernel.org/theme/css/main.css") not)

      false
    }

    "deal correctly with HTTP CONNECT requests" in {
      val model = HarReader(charles_https_har)

      model must beEmpty
    }

    "deal correctly with HTTP requests having a status=0" in {
      val model = HarReader(null_status_har)
      val requests = model.getRequests.collect { case r: RequestModel => r }
      val statuses = requests.map(_.statusCode)

      requests must have size (2) and (statuses must not contain (0))
    }

    // TODO - all the following need re- work for a valid test, and are disabled until such time

    "deal correctly with file having a websockets record" in pending {
      val scn = HarReader(play_chat_har)(ModelFixtures.config_FetchHtmlResources_false)
      val requests = scn.getRequests.collect { case r: RequestModel => r.uri }

      (scn.getRequests must have size (3)) and
        (requests must beEqualTo(List("http://localhost:9000/room", "http://localhost:9000/room?username=robert")))
    }

    ///TODO
    "have the approriate first requests" in pending {
      val scn = HarReader(play_chat_har)(ModelFixtures.config_FetchHtmlResources_false)
      val elts = scn.getRequests.toSeq
      // The first element can't be a pause.
      (elts.head must beAnInstanceOf[RequestModel]) and
        (elts.head.asInstanceOf[RequestModel].uri must beEqualTo("https://www.kernel.org/")) and
        (elts(1) must beAnInstanceOf[ExecModel]) and
        (elts(1).asInstanceOf[RequestModel].uri must beEqualTo("https://www.kernel.org/theme/css/main.css"))
    }

    "have the headers correctly set" in {

      val scn = HarReader(play_chat_har)(ModelFixtures.config_FetchHtmlResources_false)
      val elts = scn.getRequests.toSeq

      val el0 = elts.head.asInstanceOf[RequestModel]
      val el1 = elts(1).asInstanceOf[RequestModel]

      // (el0.headers must beEmpty) and  // TODO - check what this was for
      (el1.headers must not beEmpty) and
        (el1.headers must haveKeys("User-Agent", "Host", "Accept-Encoding", "Accept-Language"))
    }

    //TODO
    "return the correct number of Pause elements" in pending {
      val scn = HarReader(play_chat_har)(ModelFixtures.config_FetchHtmlResources_false)
      val elts = scn.getRequests.toSeq
      //val pauseElts = elts.collect { case PauseModel(duration) => duration }
      //pauseElts.size must beLessThan(elts.size / 2)
      false
    }

    //TODO
    "return an appropriate pause duration" in pending {

      val scn = HarReader(play_chat_har)(ModelFixtures.config_FetchHtmlResources_false)
      val elts = scn.getRequests.toSeq
      //val pauseElts = elts.collect { case PauseModel(duration) => duration }
      //val pauseDuration = pauseElts.reduce(_ + _)
      // The total duration of the HAR record is of 6454ms
      //(pauseDuration must beLessThanOrEqualTo(88389 milliseconds)) and
      //  (pauseDuration must beGreaterThan(80000 milliseconds))
      false
    }
  }

  // Deactivate Specs2 implicit to be able to use the ones provided in scala.concurrent.duration
  override def intToRichLong(v: Int) = super.intToRichLong(v)
}
