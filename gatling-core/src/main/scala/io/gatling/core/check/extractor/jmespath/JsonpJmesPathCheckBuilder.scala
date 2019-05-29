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

package io.gatling.core.check.extractor.jmespath

import io.gatling.core.check.extractor.jsonpath.JsonFilter
import io.gatling.core.session._

trait JsonpJmesPathCheckType

// we have to duplicate JmesPathCheckBuilder because traits can't take parameters (for now)
// so we can't make CheckType a parameter
trait JsonpJmesPathOfType { self: JsonpJmesPathCheckBuilder[String] =>

  def ofType[X: JsonFilter] = new JsonpJmesPathCheckBuilder[X](path, jmesPaths)
}

object JsonpJmesPathCheckBuilder {

  def jsonpJmesPath(path: Expression[String], jmesPaths: JmesPaths) =
    new JsonpJmesPathCheckBuilder[String](path, jmesPaths) with JsonpJmesPathOfType
}

class JsonpJmesPathCheckBuilder[X: JsonFilter](
    path:      Expression[String],
    jmesPaths: JmesPaths
) extends JmesPathCheckBuilderBase[JsonpJmesPathCheckType, X]("jsonpJmesPath", path, jmesPaths)
