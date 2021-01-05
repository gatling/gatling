/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.http.fetch

import io.gatling.BaseSpec
import io.gatling.http.client.uri.Uri
import io.gatling.http.fetch.InferredResourceNaming._

class InferredResourceNamingSpec extends BaseSpec {

  "UrlTrailInferredResourceNaming" should "return the url trail, query included" in {
    UrlTailInferredResourceNaming(Uri.create("http://foo.com/bar?baz=qic")) shouldBe "bar?baz=qic"
  }

  "AbsoluteUrlInferredResourceNaming" should "return the absolute url, query included" in {
    AbsoluteUrlInferredResourceNaming(Uri.create("http://foo.com/bar?baz=qic")) shouldBe "http://foo.com/bar?baz=qic"
  }

  "RelativeUrlInferredResourceNaming" should "return the relative url, query included" in {
    RelativeUrlInferredResourceNaming(Uri.create("http://foo.com/bar?baz=qic")) shouldBe "/bar?baz=qic"
  }

  "PathInferredResourceNaming" should "return full path" in {
    PathInferredResourceNaming(Uri.create("http://foo.com/bar")) shouldBe "/bar"
  }

  it should "ignore query" in {
    PathInferredResourceNaming(Uri.create("http://foo.com/bar?baz=qic")) shouldBe "/bar"
  }

  it should "not drop trailing /" in {
    PathInferredResourceNaming(Uri.create("http://foo.com/bar/")) shouldBe "/bar/"
  }

  "LastPathElementInferredResourceNaming" should "return last path element" in {
    LastPathElementInferredResourceNaming(Uri.create("http://foo.com/bla/foo.png?bar=baz")) shouldBe "foo.png"
  }

  it should "handle empty path" in {
    LastPathElementInferredResourceNaming(Uri.create("http://foo.com")) shouldBe "/"
  }

  it should "handle root path" in {
    LastPathElementInferredResourceNaming(Uri.create("http://foo.com/")) shouldBe "/"
  }

  it should "handle directory" in {
    LastPathElementInferredResourceNaming(Uri.create("http://foo.com/bar/")) shouldBe "bar/"
  }

  it should "handle sub directory" in {
    LastPathElementInferredResourceNaming(Uri.create("http://foo.com/bar/baz/")) shouldBe "baz/"
  }
}
