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

package io.gatling.core.check.jsonpath

import scala.annotation.implicitNotFound

import io.gatling.core.json.Json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType._

trait LowPriorityJsonFilterImplicits {

  private def newJsonFilter[T](f: PartialFunction[JsonNode, T]): JsonFilter[T] =
    new JsonFilter[T] {
      override val filter: PartialFunction[JsonNode, T] = f
    }

  implicit val stringJsonFilter: JsonFilter[String] = newJsonFilter { case node =>
    if (node.getNodeType == NULL) null else Json.stringifyNode(node, isRootObject = true)
  }

  implicit val jBooleanJsonFilter: JsonFilter[Boolean] = newJsonFilter {
    case node if node.getNodeType == BOOLEAN => node.booleanValue
    case node if node.getNodeType == NULL    => null.asInstanceOf[Boolean]
  }

  implicit val integerJsonFilter: JsonFilter[Int] = newJsonFilter {
    case node if node.getNodeType == NUMBER => node.intValue
    case node if node.getNodeType == NULL   => null.asInstanceOf[Int]
  }

  implicit val jLongJsonFilter: JsonFilter[Long] = newJsonFilter {
    case node if node.getNodeType == NUMBER => node.longValue
    case node if node.getNodeType == NULL   => null.asInstanceOf[Long]
  }

  implicit val jDoubleJsonFilter: JsonFilter[Double] = newJsonFilter {
    case node if node.getNodeType == NUMBER => node.doubleValue
    case node if node.getNodeType == NULL   => null.asInstanceOf[Double]
  }

  implicit val jFloatJsonFilter: JsonFilter[Float] = newJsonFilter {
    case node if node.getNodeType == NUMBER => node.floatValue
    case node if node.getNodeType == NULL   => null.asInstanceOf[Float]
  }

  implicit val jListJsonFilter: JsonFilter[Seq[Any]] = newJsonFilter {
    case node if node.getNodeType == ARRAY => Json.asScala(node).asInstanceOf[Seq[Any]]
    case node if node.getNodeType == NULL  => null.asInstanceOf[Seq[Any]]
  }

  implicit val jMapJsonFilter: JsonFilter[Map[String, Any]] = newJsonFilter {
    case node if node.getNodeType == OBJECT => Json.asScala(node).asInstanceOf[Map[String, Any]]
    case node if node.getNodeType == NULL   => null.asInstanceOf[Map[String, Any]]
  }

  implicit val anyJsonFilter: JsonFilter[Any] = newJsonFilter { case e =>
    Json.asScala(e)
  }
}

object JsonFilter extends LowPriorityJsonFilterImplicits {
  def apply[X: JsonFilter]: JsonFilter[X] = implicitly[JsonFilter[X]]
}

@implicitNotFound("No member of type class JsonFilter found for type ${X}")
trait JsonFilter[X] {
  def filter: PartialFunction[JsonNode, X]
}
