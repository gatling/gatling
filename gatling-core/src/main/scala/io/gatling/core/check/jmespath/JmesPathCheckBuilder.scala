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

package io.gatling.core.check.jmespath

import io.gatling.core.check.{ DefaultFindCheckBuilder, FindCheckBuilder, FindCriterionExtractor }
import io.gatling.core.check.jsonpath.JsonFilter
import io.gatling.core.session.Expression

import com.fasterxml.jackson.databind.JsonNode

abstract class JmesPathCheckBuilderBase[T, X: JsonFilter](
    name: String,
    private[jmespath] val path: Expression[String],
    private[jmespath] val jmesPaths: JmesPaths
) extends DefaultFindCheckBuilder[T, JsonNode, X](path.map(new JmesPathExtractor(name, _, jmesPaths)), displayActualValue = true)

class JmesPathExtractor[X: JsonFilter](name: String, path: String, jmesPaths: JmesPaths)
    extends FindCriterionExtractor[JsonNode, String, X](name, path, 0, jmesPaths.extract(_, path))

trait JmesPathCheckType

trait JmesPathOfType { self: JmesPathCheckBuilder[String] =>

  def ofType[X: JsonFilter]: FindCheckBuilder[JmesPathCheckType, JsonNode, X] = new JmesPathCheckBuilder[X](path, jmesPaths)
}

object JmesPathCheckBuilder {

  def jmesPath(path: Expression[String], jmesPaths: JmesPaths): JmesPathCheckBuilder[String] with JmesPathOfType =
    new JmesPathCheckBuilder[String](path, jmesPaths) with JmesPathOfType
}

class JmesPathCheckBuilder[X: JsonFilter](
    path: Expression[String],
    jmesPaths: JmesPaths
) extends JmesPathCheckBuilderBase[JmesPathCheckType, X]("jmesPath", path, jmesPaths)
