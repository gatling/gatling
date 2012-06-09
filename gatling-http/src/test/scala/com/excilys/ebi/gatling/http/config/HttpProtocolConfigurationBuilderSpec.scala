package com.excilys.ebi.gatling.http.config

import org.specs2.mutable.Specification
import com.ning.http.client.{Response, Request}

/**
 * HttpProtocolConfigurationBuilderSpec is responsible for
 */

class HttpProtocolConfigurationBuilderSpec extends Specification {

  "http protocol configuration builder" should {
    "support an optional extra request info extractor" in {

      val expectedExtractor: (Request => List[String]) = {
        (Request) => List()
      }

      val builder = HttpProtocolConfigurationBuilder.httpConfig
        .disableWarmUp
        .requestInfoExtractor(expectedExtractor)
      val config: HttpProtocolConfiguration = builder.build

      config.extraRequestInfoExtractor.get should beEqualTo(expectedExtractor)
    }

    "support an optional extra response info extractor" in {

      val expectedExtractor: (Response => List[String]) = {
        (Response) => List()
      }

      val builder = HttpProtocolConfigurationBuilder.httpConfig
        .disableWarmUp
        .responseInfoExtractor(expectedExtractor)
      val config: HttpProtocolConfiguration = builder.build

      config.extraResponseInfoExtractor.get should beEqualTo(expectedExtractor)
    }
  }
}
