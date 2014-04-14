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
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

/**
 * @author Ivan Mushketyk
 */
@RunWith(classOf[JUnitRunner])
class SitemapParserTest extends Specification {

  def getFile(filePath: String) = getClass.getClassLoader.getResourceAsStream(filePath)

  "sitemap parser" should {
    "parse valid sitemap file" in {
      val records = SitemapParser.parse(getFile("sitemap.xml")).toArray

      records.size should be equalTo 5

      records(0) should be equalTo Map(
        "loc" -> "http://www.example.com/",
        "lastmod" -> "2005-01-01",
        "changefreq" -> "monthly",
        "priority" -> "0.8")

      records(1) should be equalTo Map(
        "loc" -> "http://www.example.com/catalog?item=12&amp;desc=vacation_hawaii",
        "changefreq" -> "weekly")

      records(2) should be equalTo Map(
        "loc" -> "http://www.example.com/catalog?item=73&amp;desc=vacation_new_zealand",
        "lastmod" -> "2004-12-23",
        "changefreq" -> "weekly")

      records(3) should be equalTo Map(
        "loc" -> "http://www.example.com/catalog?item=74&amp;desc=vacation_newfoundland",
        "lastmod" -> "2004-12-23T18:00:15+00:00",
        "priority" -> "0.3")

      records(4) should be equalTo Map(
        "loc" -> "http://www.example.com/catalog?item=83&amp;desc=vacation_usa",
        "lastmod" -> "2004-11-23")
    }

    "throw exception when loc is missing" in {
      SitemapParser.parse(getFile("sitemap_loc_missing.xml")) must throwA[SitemapFormatException]
    }
  }
}
