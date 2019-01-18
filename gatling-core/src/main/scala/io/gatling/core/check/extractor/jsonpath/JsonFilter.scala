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

package io.gatling.core.check.extractor.jsonpath

import scala.annotation.implicitNotFound

import io.gatling.core.json.Json

trait LowPriorityJsonFilterImplicits {

  private def newJsonFilter[T](f: PartialFunction[Any, T]): JsonFilter[T] =
    new JsonFilter[T] {
      override val filter: PartialFunction[Any, T] = f
    }

  implicit val stringJsonFilter: JsonFilter[String] = newJsonFilter {
    case e: Any => Json.stringify(e)
    case null   => null
  }

  implicit val jBooleanJsonFilter: JsonFilter[Boolean] = newJsonFilter {
    case e: java.lang.Boolean => e
    case null                 => null.asInstanceOf[Boolean]
  }

  implicit val integerJsonFilter: JsonFilter[Int] = newJsonFilter {
    case e: Number => e.intValue
    case null      => null.asInstanceOf[Int]
  }

  implicit val jLongJsonFilter: JsonFilter[Long] = newJsonFilter {
    case e: Number => e.longValue
    case null      => null.asInstanceOf[Long]
  }

  implicit val jDoubleJsonFilter: JsonFilter[Double] = newJsonFilter {
    case e: Number => e.doubleValue
    case null      => null.asInstanceOf[Double]
  }

  implicit val jFloatJsonFilter: JsonFilter[Float] = newJsonFilter {
    case e: Number => e.floatValue
    case null      => null.asInstanceOf[Float]
  }

  implicit val jListJsonFilter: JsonFilter[Seq[Any]] = newJsonFilter {
    case e: java.util.List[_] => Json.asScala(e).asInstanceOf[Seq[Any]]
    case null                 => null.asInstanceOf[Seq[Any]]
  }

  implicit val jMapJsonFilter: JsonFilter[Map[String, Any]] = newJsonFilter {
    case e: java.util.Map[_, _] => Json.asScala(e).asInstanceOf[Map[String, Any]]
    case null                   => null.asInstanceOf[Map[String, Any]]
  }

  implicit val anyJsonFilter: JsonFilter[Any] = newJsonFilter {
    case e => Json.asScala(e)
  }
}

object JsonFilter extends LowPriorityJsonFilterImplicits {
  def apply[X: JsonFilter]: JsonFilter[X] = implicitly[JsonFilter[X]]
}

@implicitNotFound("No member of type class JsonFilter found for type ${X}")
trait JsonFilter[X] {
  def filter: PartialFunction[Any, X]
}
