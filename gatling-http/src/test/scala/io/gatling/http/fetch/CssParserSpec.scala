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

import java.net.URI

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CssParserSpec extends Specification {

	val rootURI = new URI("http://akka.io/")

	"parsing CSS" should {

		def rulesUri(css: String) = CssParser.extractResources(rootURI, css).map(_.url)

		"handle an empty CSS" in {
			rulesUri("") must beEqualTo(Nil)
		}

		"fetch imports" in {
			val css = """
				@import url("import1.css");
				body{background-image: url('backgrounds/blizzard.png');}
				@import url("import2.css");
				"""
			rulesUri(css) must beEqualTo(Seq("http://akka.io/import1.css", "http://akka.io/import2.css"))
		}

		"ignore commented imports with a simple CSS" in {
			val css = """
				/*@import url("import1.css");*/
				body{background-image: url('backgrounds/blizzard.png');}
				@import url("import2.css");
				"""
			rulesUri(css) must beEqualTo(Seq("http://akka.io/import2.css"))
		}
	}
}
