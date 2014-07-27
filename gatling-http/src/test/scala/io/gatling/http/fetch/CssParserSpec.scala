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
package io.gatling.http.fetch

import org.scalatest.{ FlatSpec, Matchers }

import com.ning.http.client.uri.UriComponents

class CssParserSpec extends FlatSpec with Matchers {

  val rootURI = UriComponents.create("http://akka.io/")

  def rulesUri(css: String) = CssParser.extractResources(rootURI, css).map(_.url)

  "parsing CSS" should "handle an empty CSS" in {
    rulesUri("") shouldBe empty
  }

  it should "fetch imports" in {
    val css = """
        @import url("import1.css");
        body{background-image: url('backgrounds/blizzard.png');}
        @import url("import2.css");"""

    rulesUri(css) shouldBe Seq("http://akka.io/import1.css", "http://akka.io/import2.css")
  }

  it should "ignore commented imports with a simple CSS" in {
    val css = """
        /*@import url("import1.css");*/
        body{background-image: url('backgrounds/blizzard.png');}
        @import url("import2.css");"""

    rulesUri(css) shouldBe Seq("http://akka.io/import2.css")
  }
}
