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
package io.gatling.commons.util

import io.gatling.BaseSpec
import io.gatling.commons.util.PathHelper._

class PathHelperSpec extends BaseSpec {

  val root = string2path("foo")

  "ancestor" should "throw an IllegalArgumentException when ancestor rank is negative" in {
    an[IllegalArgumentException] should be thrownBy root.ancestor(-1)
  }

  it should "throw an IllegalArgumentException when asked rank > nb of parents" in {
    an[IllegalArgumentException] should be thrownBy (root / "bar").ancestor(3)
  }

  it should "get the parent of rank n otherwise" in {
    (root / "foo" / "bar").ancestor(1) shouldBe (root / "foo")
  }

  "extension" should "return an empty String when the specified path has no extension" in {
    root.extension shouldBe ""
  }

  it should "return the file extension if the specified path has one" in {
    (root / "foo.json").extension shouldBe "json"
  }

  "hasExtension" should "return true if the file has one of the specified extension, ignoring case" in {
    (root / "foo.json").hasExtension("json") shouldBe true
    (root / "foo.json").hasExtension("JSON") shouldBe true
    (root / "foo.json").hasExtension("sql", "mp3", "JSON") shouldBe true
  }

  it should "return false if the file has none of the specified extensions" in {
    (root / "foo.json").hasExtension("sql") shouldBe false
  }

  "stripExtension" should "not modify the path if it has no extension" in {
    root.stripExtension shouldBe "foo"
  }

  it should "remove the file extension if the specified path has one" in {
    string2path("foo.json").stripExtension shouldBe "foo"
  }
}
