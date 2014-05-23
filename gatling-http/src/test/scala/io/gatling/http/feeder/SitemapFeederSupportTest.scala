/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.feeder

import org.specs2.mutable.Specification
import scala.reflect.io.File
import io.gatling.core.config.FileResource
import io.gatling.core.validation.{ Success, Failure }
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

/**
 * @author Ivan Mushketyk
 */
@RunWith(classOf[JUnitRunner])
class SitemapFeederSupportTest extends Specification with SitemapFeederSupport {

  def getFile(filePath: String) = File(getClass.getClassLoader.getResource("sitemap.xml").getFile)

  "create sitemap feeder" should {
    "get file resource" in {
      val success = Success(FileResource(getFile("sitemap.xml")))
      val feederBuilder = sitemap(success)

      feederBuilder.records.size should be equalTo 5
    }

    "get non existing resource" in {
      val failure = Failure("error")
      sitemap(failure) must throwA[IllegalArgumentException]
    }
  }

}
