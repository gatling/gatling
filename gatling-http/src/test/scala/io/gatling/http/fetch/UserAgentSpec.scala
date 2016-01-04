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
package io.gatling.http.fetch

import io.gatling.BaseSpec

class UserAgentSpec extends BaseSpec {

  "UserAgent.parseFromHeader" should "extract IE 9.0 version" in {
    val agent = UserAgent.parseFromHeader("Mozilla/5.0 (Windows; U; MSIE 9.0; WIndows NT 9.0; en-US))")
    agent shouldBe Some(UserAgent(UserAgent.IE, 9.0f))
  }

  it should "extract IE 8.0 version" in {
    val agent = UserAgent.parseFromHeader("Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; GTB7.4; InfoPath.2; SV1; .NET CLR 3.3.69573; WOW64; en-US)")
    agent shouldBe Some(UserAgent(UserAgent.IE, 8.0f))
  }

  it should "don't parse Firefox version" in {
    val agent = UserAgent.parseFromHeader("Mozilla/5.0 (X11; OpenBSD amd64; rv:28.0) Gecko/20100101 Firefox/28.0")
    agent shouldBe None
  }
}
