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
package io.gatling.recorder.export.template

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import io.gatling.recorder.model.ModelFixtures


@RunWith(classOf[JUnitRunner])
class TemplateSpec extends Specification {

  def resourceAsString(p: String) = {
    val is = getClass.getClassLoader.getResourceAsStream(p)
    scala.io.Source.fromInputStream(is).getLines().mkString("\n")
  }
  
  "Export Templates" should {
    
    "render simulation correctly with basic model" in {

      val rendered = SimulationTemplate.render(ModelFixtures.basicModel)

      rendered(0)._2.equals(resourceAsString("simulation.txt"))
    }
    
    
    "render scenario correctly with basic model" in {

      val rendered = ScenarioTemplate.render(ModelFixtures.basicModel)

      rendered(0)._2.equals(resourceAsString("scenarios.txt"))
    }

    "render navigations correctly with basic model" in {

      val rendered = NavigationTemplate.render(ModelFixtures.basicModel)

      rendered(0)._2.equals(resourceAsString("navigations.txt"))
    }

    "render requests correctly with basic model" in {

      val rendered = RequestTemplate.render(ModelFixtures.basicModel)

      rendered(0)._2.equals(resourceAsString("requests.txt"))
    }

    "render protocol correctly with basic model" in {

      val rendered = ProtocolTemplate.render(ModelFixtures.basicModel)

      rendered(0)._2.equals(resourceAsString("protocol.txt"))
    }

    "render request body correctly with basic model" in {

      val rendered = RequestBodyTemplate.render(ModelFixtures.basicModel)

      new String(rendered(0)._2).equals(resourceAsString("request_body_main3.txt"))
    }
    
// TODO - further tests needed
//    "render simulation correctly with basic model no outgoing proxy" in {
//
//      ModelFixtures.config = ModelFixtures.config_no_proxy
//      
//      val rendered = SimulationTemplate.render(ModelFixtures.basicModel)
//
//      rendered(0)._2.equals(resourceAsString("simulation_no_outgoing_proxy.txt"))
//    }
//    
//    "render navigations correctly with basic model and filters" in {
//
//      ModelFixtures.config = ModelFixtures.config_no_proxy_filter
//      
//      val rendered = NavigationTemplate.render(ModelFixtures.basicModel)
//
//      rendered(0)._2.equals(resourceAsString("navigations_filters.txt"))
//    }
//
//    "render requests correctly with basic model and filters" in {
//
//      ModelFixtures.config = ModelFixtures.config_no_proxy_filter
//      
//      val rendered = RequestTemplate.render(ModelFixtures.basicModel)
//
//      rendered(0)._2.equals(resourceAsString("requests_filters.txt"))
//    }
//
//    "render protocol correctly with basic model and filters" in {
//
//      ModelFixtures.config = ModelFixtures.config_no_proxy_filter
//      
//      val rendered = ProtocolTemplate.render(ModelFixtures.basicModel)
//
//      rendered(0)._2.equals(resourceAsString("protocol_filters.txt"))
//    }
}
}