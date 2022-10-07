/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.core.util

import io.gatling.BaseSpec

class ResourceSpec extends BaseSpec {
  "cleanResourcePath" should "leave correct path unchanged" in {
    Resource.cleanResourcePath("data/file.csv") shouldBe "data/file.csv"
  }

  it should "fix relative src/test/resources" in {
    Resource.cleanResourcePath("src/test/resources/data/file.csv") shouldBe "data/file.csv"
  }

  it should "fix relative src/main/resources" in {
    Resource.cleanResourcePath("src/main/resources/data/file.csv") shouldBe "data/file.csv"
  }

  it should "fix relative src/gatling/resources" in {
    Resource.cleanResourcePath("src/gatling/resources/data/file.csv") shouldBe "data/file.csv"
  }

  it should "fix relative ./src/test/resources" in {
    Resource.cleanResourcePath("src/test/resources/data/file.csv") shouldBe "data/file.csv"
  }

  it should "fix absolute src/test/resources" in {
    Resource.cleanResourcePath("/Users/user/dir/src/test/resources/data/file.csv") shouldBe "data/file.csv"
  }

  it should "fix absolute src/main/resources" in {
    Resource.cleanResourcePath("/Users/user/dir/src/main/resources/data/file.csv") shouldBe "data/file.csv"
  }

  it should "fix absolute src/gatling/resources" in {
    Resource.cleanResourcePath("/Users/user/dir/src/gatling/resources/data/file.csv") shouldBe "data/file.csv"
  }
}
