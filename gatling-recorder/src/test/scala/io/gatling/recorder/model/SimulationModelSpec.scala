package io.gatling.recorder.model

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import io.gatling.recorder.export.template._

@RunWith(classOf[JUnitRunner])
class SimulationModelSpec extends Specification {

  "Simulation Model" should {

//        "remove HTTP redirection " in {
//      val r1 = RequestElement("http://gatling.io/", "GET", Map.empty, None, 200, List.empty,Option(""))
//      val r2 = RequestElement("http://gatling.io/rn1.html", "GET", Map.empty, None, 302, List.empty,Option(""))
//      val r3 = RequestElement("http://gatling.io/release-note-1.html", "GET", Map.empty, None, 200, List.empty,Option(""))
//      val r4 = RequestElement("http://gatling.io/details.html", "GET", Map.empty, None, 200, List.empty,Option(""))
//
//      val scn = ScenarioDefinition(List(1l -> r1, 2l -> r2, 3l -> r3, 4l -> r4), List.empty)
//      scn.elements should beEqualTo(List(r1, r2.copy(statusCode = 200), r4))
//    }
//
    
    "process redirects with FollowRedirect = true" in {

      ModelFixtures.config = ModelFixtures.config_basic
      
      false

    }
    
    "process redirects with FollowRedirect = false" in {

      false

    }
    
    
//    "filter out embedded resources of HTML documents" in {
//      val r1 = RequestElement("http://gatling.io", "GET", Map.empty, None, 200,
//        List(CssResource(new URI("http://gatling.io/main.css")), RegularResource(new URI("http://gatling.io/img.jpg"))),Option(""))
//      val r2 = RequestElement("http://gatling.io/main.css", "GET", Map.empty, None, 200, List.empty,Option(""))
//      val r3 = RequestElement("http://gatling.io/details.html", "GET", Map(CONTENT_TYPE -> "text/html;charset=UTF-8"), None, 200, List.empty,Option(""))
//      val r4 = RequestElement("http://gatling.io/img.jpg", "GET", Map.empty, None, 200, List.empty,Option(""))
//      val r5 = RequestElement("http://gatling.io", "GET", Map.empty, None, 200,
//        List(CssResource(new URI("http://gatling.io/main.css"))),Option(""))
//      val r6 = RequestElement("http://gatling.io/main.css", "GET", Map.empty, None, 200, List.empty,Option(""))
//
//      val scn = ScenarioDefinition(List(1l -> r1, 2l -> r2, 3l -> r3, 4l -> r4, 5l -> r5, 6l -> r6), List.empty)
//      scn.elements should beEqualTo(List(r1, r3, r5))
//    }
    
    "process html resources with FetchHtmlResources = true" in {

      false

    }

    "process html resources with FetchHtmlResources = false" in {

      false

    }
    
    "process referers with AutomaticReferer = true" in {

      false

    }
    
    "process referers with AutomaticReferer = true" in {

      false

    }
    
    
  }
}