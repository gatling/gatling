/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import java.io.File

import io.gatling.BaseSpec
import io.gatling.commons.validation.{ Failure, Success }
import io.gatling.core.util.FileResource

class SitemapFeederSupportSpec extends BaseSpec with SitemapFeederSupport {

  def getFile(filePath: String) = new File(getClass.getClassLoader.getResource("sitemap.xml").getFile)

  "create sitemap feeder" should "get file resource" in {
    val success = Success(FileResource(getFile("sitemap.xml")))
    val feederBuilder = sitemap(success)

    feederBuilder.records should have size 5
  }

  it should "get non existing resource" in {
    val failure = Failure("error")
    a[IllegalArgumentException] shouldBe thrownBy(sitemap(failure))
  }
}
