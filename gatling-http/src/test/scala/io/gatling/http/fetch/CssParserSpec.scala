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

		"work with an empty CSS" in {
			rulesUri("") must beEqualTo(Nil)
		}

		"work with a simple CSS" in {
			val css = """body{background-image: url('backgrounds/blizzard.png');}"""
			rulesUri(css) must beEqualTo(Seq("http://akka.io/backgrounds/blizzard.png"))

			val css2 = """body { 
										background-image: url('backgrounds/blizzard.png'); 
								 }"""
			rulesUri(css2) must beEqualTo(Seq("http://akka.io/backgrounds/blizzard.png"))
		}

		"work with a all kind of selectors CSS" in {
			val css = """.foo { background-image: url('backgrounds/blizzard.png');}"""
			rulesUri(css) must beEqualTo(Seq("http://akka.io/backgrounds/blizzard.png"))

			val css2 = """#foo { background-image: url('backgrounds/blizzard.png');}"""
			rulesUri(css2) must beEqualTo(Seq("http://akka.io/backgrounds/blizzard.png"))

			val css3 = """.foo .bar { background-image: url('backgrounds/blizzard.png');}"""
			rulesUri(css3) must beEqualTo(Seq("http://akka.io/backgrounds/blizzard.png"))
		}

		"work with a complex CSS background directive" in {
			val css = """body {background:#ffffff url('img_tree.png') no-repeat right top;}"""
			rulesUri(css) must beEqualTo(Seq("http://akka.io/img_tree.png"))
		}

		"work with a multiple urls in the CSS" in {
			val css = """body { background-image: url('backgrounds/blizzard.png'); }
				    			.foo { background: url('backgrounds/landscapes/sky.jpg'); }"""
			rulesUri(css) must beEqualTo(Seq("http://akka.io/backgrounds/blizzard.png", "http://akka.io/backgrounds/landscapes/sky.jpg"))
		}

		// See http://www.w3.org/TR/CSS2/syndata.html#value-def-uri for details
		"work with all URI format" in {
			// With double quotes
			val css = """body { background: url("http://www.example.com/pinkish.png") }"""
			rulesUri(css) must beEqualTo(Seq("http://www.example.com/pinkish.png"))

			// With simple quotes
			val css2 = """li { list-style: url(http://www.example.com/redball.png) disc }"""
			rulesUri(css2) must beEqualTo(Seq("http://www.example.com/redball.png"))

			// With nothing
			val css3 = """body { background: url(http://www.example.com/pinkish.png) }"""
			rulesUri(css3) must beEqualTo(Seq("http://www.example.com/pinkish.png"))

			// With spaces
			val css4 = """body { background: url( http://www.example.com/pinkish.png ) }"""
			rulesUri(css4) must beEqualTo(Seq("http://www.example.com/pinkish.png"))
		}
	}
}
