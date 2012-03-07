/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.check.extractor.json

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import com.excilys.ebi.gatling.core.check.extractor.json.JsonTokenizer.tokenize
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JsonTokenizerSpec extends Specification {

	"//a" should {

		"produce (SimpleNode(\"a\"), JsonRootWildCard)" in {
			tokenize("//a") must equalTo(List(SimpleNode("a"), JsonRootWildCard))
		}
	}

	"//a/b" should {

		"produce (SimpleNode(\"b\"), SimpleNode(\"a\"), JsonRootWildCard)" in {
			tokenize("//a/b") must equalTo(List(SimpleNode("b"), SimpleNode("a"), JsonRootWildCard))
		}
	}

	"/a/*" should {

		"produce (NodeWildCard, SimpleNode(\"a\"))" in {
			tokenize("/a/*") must equalTo(List(NodeWildCard, SimpleNode("a")))
		}
	}

	"/*/a" should {

		"produce (SimpleNode(\"a\"), NodeWildCard)" in {
			tokenize("/*/a") must equalTo(List(SimpleNode("a"), NodeWildCard))
		}
	}

	"/a[1]" should {

		"produce (ArrayElementNode(\"a\", 1))" in {
			tokenize("/a[1]") must equalTo(List(ArrayElementNode("a", 1)))
		}
	}

	"/a[1]/b" should {

		"produce (SimpleNode(\"b\"), ArrayElementNode(\"a\", 1))" in {
			tokenize("/a[1]/b") must equalTo(List(SimpleNode("b"), ArrayElementNode("a", 1)))
		}
	}
	
		"a" should {

		"throw a JsonTokenizerException" in {
			tokenize("a") must throwA[JsonTokenizerException]
		}
	}

	//	println(tokenize("//*"));
	//		println(tokenize("BBB"));
}