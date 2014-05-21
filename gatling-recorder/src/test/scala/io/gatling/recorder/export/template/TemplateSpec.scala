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
      val expected = ""

      rendered(0)._2.equals(resourceAsString("protocol.txt"))

    }

    "render request body correctly with basic model" in {

      val rendered = RequestBodyTemplate.render(ModelFixtures.basicModel)

      new String(rendered(0)._2).equals(resourceAsString("request_body_main3.txt"))

    }
    
}
}