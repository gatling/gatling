package com.excilys.ebi.gatling.http.config

import org.specs2.mutable.Specification
import com.ning.http.client.Request

/**
 * HttpProtocolConfigurationBuilderSpec is responsible for
 */

class HttpProtocolConfigurationBuilderSpec extends Specification {

  "builder" should {
    "support an optional extra request info extractor" in {

      val expectedExtractor: (Request => List[String]) = {
        (Request) => List()
      }

      val builder = HttpProtocolConfigurationBuilder.httpConfig.extraRequestInfoExtractor(expectedExtractor)
      val config: HttpProtocolConfiguration = HttpProtocolConfigurationBuilder.toHttpProtocolConfiguration(builder)

      config.extraRequestInfoExtractor.get should beEqualTo(expectedExtractor)
    }
  }
}
