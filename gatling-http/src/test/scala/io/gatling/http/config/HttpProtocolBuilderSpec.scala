/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.config

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.message.Status
import io.gatling.core.session.Session
import io.gatling.http.response.Response
import com.ning.http.client.Request

@RunWith(classOf[JUnitRunner])
class HttpProtocolBuilderSpec extends Specification {

	GatlingConfiguration.setUp()

	"http protocol configuration builder" should {
		"support an optional extra info extractor" in {

			val expectedExtractor = (requestName: String, status: Status, session: Session, request: Request, response: Response) => Nil

			val builder = HttpProtocolBuilder.default
				.disableWarmUp
				.extraInfoExtractor(expectedExtractor)
			val config: HttpProtocol = builder.build

			config.extraInfoExtractor.get should beEqualTo(expectedExtractor)
		}

		"be able to support a base URL" in {
			val url = "http://url"

			val builder = HttpProtocolBuilder.default
				.baseURL(url)
				.disableWarmUp

			val config: HttpProtocol = builder.build

			Seq(config.baseURL.get, config.baseURL.get, config.baseURL.get) must be equalTo (Seq(url, url, url))
		}

		"provide a Round-Robin strategy when multiple urls are provided" in {
			val url1 = "http://url1"
			val url2 = "http://url2"

			val builder = HttpProtocolBuilder.default
				.baseURLs(url1, url2)
				.disableWarmUp

			val config: HttpProtocol = builder.build

			Seq(config.baseURL.get, config.baseURL.get, config.baseURL.get) must be equalTo (Seq(url1, url2, url1))
		}
	}
}
