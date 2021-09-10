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

package io.gatling.recorder.convert

import io.gatling.BaseSpec
import io.gatling.recorder.convert.RequestElement.extractCharsetFromContentType

class RequestElementSpec extends BaseSpec {

  "extractCharsetFromContentType" should "extract unwrapped charset from Content-Type" in {
    extractCharsetFromContentType("text/html; charset=utf-8") shouldBe Some("utf-8")
  }

  it should "extract wrapped charset from Content-Type" in {
    extractCharsetFromContentType("text/html; charset=\"utf-8\"") shouldBe Some("utf-8")
  }

  it should "not extract when Content-Type doesn't have a charset attribute" in {
    extractCharsetFromContentType("text/html") shouldBe None
  }
}
