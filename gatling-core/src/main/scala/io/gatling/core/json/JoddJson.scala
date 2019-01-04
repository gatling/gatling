/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.core.json

import io.gatling.core.jodd.FixedJoddJsonParser

import jodd.json.{ JsonParser => JoddJsonParser }

class JoddJson {

  private val parsers: ThreadLocal[JoddJsonParser] = ThreadLocal.withInitial(() => new FixedJoddJsonParser)

  def parse(string: String): AnyRef = parsers.get().parse(string)
}
