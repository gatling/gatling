package io.gatling.recorder.model

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import io.gatling.recorder.export.template._

@RunWith(classOf[JUnitRunner])
class SimulationModelSpec extends Specification {

  "Simulation Model" should {

    // TODO - requires more work.
    "remove HTTP redirection with redirectingModel" in pending {

      val model = ModelFixtures.redirectingModel
      val a_redirecting_request = RequestModel("http://gatling.io/main2-302.css", "GET", Map.empty, None, 302, List.empty, Option(""))
      ! model.getRequests.contains(a_redirecting_request)

    }

  }
}
