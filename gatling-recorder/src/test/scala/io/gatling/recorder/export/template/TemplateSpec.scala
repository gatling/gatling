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
    
    "render simulation correctly" in {

      val rendered = SimulationTemplate.render(ModelFixtures.basicModel)

      println(rendered)
      rendered.equals(resourceAsString("template/simulation.txt"))

    }
    
    
    "render scenario correctly" in {

      val rendered = ScenarioTemplate.render(ModelFixtures.basicModel)
      val expected = ""

      println(rendered)
      rendered.equals(expected)

    }

    "render navigations correctly" in {

      val rendered = NavigationTemplate.render(ModelFixtures.basicModel)
      val expected = ""

      println(rendered)
      rendered.equals(expected)

    }

    "render requests correctly" in {

      val rendered = RequestTemplate.render(ModelFixtures.basicModel)
      val expected = ""

      println(rendered)
      rendered.equals(expected)

    }

    "render protocol correctly" in {

      val rendered = ProtocolTemplate.render(ModelFixtures.basicModel)
      val expected = ""

      println(rendered)
      rendered.equals(expected)

    }

    "render request body correctly" in {

      val rendered = RequestBodyTemplate.render(ModelFixtures.basicModel)
      val expected = ""

      println("request body " + rendered + " : " + new String(rendered(0)._2))
      rendered.equals(expected)

    }
    
}
}