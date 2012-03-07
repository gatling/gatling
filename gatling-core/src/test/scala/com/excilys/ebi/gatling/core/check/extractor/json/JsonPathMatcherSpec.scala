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
import com.excilys.ebi.gatling.core.check.extractor.json.JsonPathMatcher.matchPath
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JsonPathMatcherSpec extends Specification {

	"//a" should {

		"accept /a" in {
			matchPath("//a", "/a") must beTrue
		}

		"reject /b" in {
			matchPath("//a", "/b") must beFalse
		}

		"accept /b/a" in {
			matchPath("//a", "/b/a") must beTrue
		}

		"reject /a/b" in {
			matchPath("//a", "/a/b") must beFalse
		}
	}

	"//a/b" should {

		"accept /a/b" in {
			matchPath("//a/b", "/a/b") must beTrue
		}

		"reject /a/b/c" in {
			matchPath("//a/b", "/a/b/c") must beFalse
		}

		"accept /c/a/b" in {
			matchPath("//a/b", "/c/a/b") must beTrue
		}

		"reject /c/a/b/c" in {
			matchPath("//a/c", "/c/a/b/d") must beFalse
		}
	}

	"/a/*" should {

		"accept /a/b" in {
			matchPath("/a/*", "/a/b") must beTrue
		}

		"reject /a" in {
			matchPath("/a/*", "/a") must beFalse
		}

		"reject /b/a/c" in {
			matchPath("/a/*", "/b/a/c") must beFalse
		}
	}

	"//a/*" should {

		"accept /b/a/c" in {
			matchPath("//a/*", "/b/a/c") must beTrue
		}
	}

	"/a/b/*" should {

		"accept /a/b/c" in {
			matchPath("/a/b/*", "/a/b/c") must beTrue
		}

		"reject /a/b" in {
			matchPath("/a/b/*", "/a/b") must beFalse
		}
	}

	"/*/a" should {

		"accept /b/a" in {
			matchPath("/*/a", "/b/a") must beTrue
		}

		"reject /a" in {
			matchPath("/*/a", "/a") must beFalse
		}
		"reject /a/b" in {
			matchPath("/*/a", "/a/b") must beFalse
		}
	}

	"/*/*/a" should {

		"accept /b/c/a" in {
			matchPath("/*/*/a", "/b/c/a") must beTrue
		}

		"reject /b/a" in {
			matchPath("/*/*/a", "/b/a") must beFalse
		}
	}

	"/a[1]" should {

		"accept /a[1]" in {
			matchPath("/a[1]", "/a[1]") must beTrue
		}

		"reject /a[2]" in {
			matchPath("/a[1]", "/a[2]") must beFalse
		}

		"reject /a" in {
			matchPath("/a[1]", "/a") must beFalse
		}
	}

	"/a[1]/b" should {

		"accept /a[1]/b" in {
			matchPath("/a[1]/b", "/a[1]/b") must beTrue
		}

		"reject /a[2]/b" in {
			matchPath("/a[1]/b", "/a[2]/b") must beFalse
		}

		"reject /a/b" in {
			matchPath("/a[1]/b", "/a/b") must beFalse
		}
	}
}