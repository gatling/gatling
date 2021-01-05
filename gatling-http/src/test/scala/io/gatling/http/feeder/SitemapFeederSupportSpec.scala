/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.feeder

import java.nio.file.Paths

import io.gatling.BaseSpec
import io.gatling.commons.validation.Failure
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.Resource

class SitemapFeederSupportSpec extends BaseSpec with SitemapFeederSupport {

  private implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  "create sitemap feeder" should "get file resource" in {
    val feederBuilder = sitemap(Resource.resolveResource(Paths.get(""), "sitemap.xml"))
    feederBuilder.readRecords.length shouldBe 5
  }

  it should "get non existing resource" in {
    val failure = Failure("error")
    a[IllegalArgumentException] shouldBe thrownBy(sitemap(failure))
  }
}
