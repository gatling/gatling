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
package io.gatling.http.dom

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CssParserSpec extends Specification {

	"parsing CSS" should {

		"work with an empty CSS" in {
			CssParser.extractSelectorsAndUrls("") must beEqualTo(Seq())
		}

		"work with a simple CSS" in {
			val css = """body{background-image: url('backgrounds/blizzard.png');}"""
			CssParser.extractSelectorsAndUrls(css) must beEqualTo(Seq("body" -> "backgrounds/blizzard.png"))

			val css2 = """body { 
										background-image: url('backgrounds/blizzard.png'); 
								 }"""
			CssParser.extractSelectorsAndUrls(css2) must beEqualTo(Seq("body" -> "backgrounds/blizzard.png"))
		}

		"work with a all kind of selectors CSS" in {
			val css = """.foo { background-image: url('backgrounds/blizzard.png');}"""
			CssParser.extractSelectorsAndUrls(css) must beEqualTo(Seq(".foo" -> "backgrounds/blizzard.png"))

			val css2 = """#foo { background-image: url('backgrounds/blizzard.png');}"""
			CssParser.extractSelectorsAndUrls(css2) must beEqualTo(Seq("#foo" -> "backgrounds/blizzard.png"))

			val css3 = """.foo .bar { background-image: url('backgrounds/blizzard.png');}"""
			CssParser.extractSelectorsAndUrls(css3) must beEqualTo(Seq(".foo .bar" -> "backgrounds/blizzard.png"))
		}

		"work with a complex CSS background directive" in {
			val css = """body {background:#ffffff url('img_tree.png') no-repeat right top;}"""
			CssParser.extractSelectorsAndUrls(css) must beEqualTo(Seq("body" -> "img_tree.png"))
		}

		"work with a multiple urls in the CSS" in {
			val css = """body { background-image: url('backgrounds/blizzard.png'); }
				    			.foo { background: url('backgrounds/landscapes/sky.jpg'); }"""
			CssParser.extractSelectorsAndUrls(css) must beEqualTo(Seq("body" -> "backgrounds/blizzard.png", ".foo" -> "backgrounds/landscapes/sky.jpg"))
		}

		// See http://www.w3.org/TR/CSS2/syndata.html#value-def-uri for details
		"work with all URI format" in {
			// With double quotes
			val css = """body { background: url("http://www.example.com/pinkish.png") }"""
			CssParser.extractSelectorsAndUrls(css) must beEqualTo(Seq("body" -> "http://www.example.com/pinkish.png"))
			
			// With simple quotes
			val css2 = """li { list-style: url(http://www.example.com/redball.png) disc }"""
			CssParser.extractSelectorsAndUrls(css2) must beEqualTo(Seq("li" -> "http://www.example.com/redball.png"))
			
			// With nothing
			val css3 =  """body { background: url(http://www.example.com/pinkish.png) }"""
			CssParser.extractSelectorsAndUrls(css3) must beEqualTo(Seq("body" -> "http://www.example.com/pinkish.png"))
			
			// With spaces
			val css4 =  """body { background: url( http://www.example.com/pinkish.png ) }"""
			CssParser.extractSelectorsAndUrls(css4) must beEqualTo(Seq("body" -> "http://www.example.com/pinkish.png"))
			
		}
		
	}
}
