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
package io.gatling.core.check.extractor.jsonpath

import scala.annotation.implicitNotFound
import scala.collection.JavaConversions.{ asScalaBuffer, mapAsScalaMap }
import scala.collection.breakOut

import io.gatling.core.json.Json

trait LowPriorityJsonFilterImplicits {

  implicit val stringJsonFilter = new JsonFilter[String] {
    val filter: PartialFunction[Any, String] = {
      case e: Any => Json.stringify(e)
      case null   => null
    }
  }

  implicit val jBooleanJsonFilter = new JsonFilter[Boolean] {
    val filter: PartialFunction[Any, Boolean] = {
      case e: java.lang.Boolean => e
      case null                 => null.asInstanceOf[Boolean]
    }
  }

  implicit val integerJsonFilter = new JsonFilter[Int] {
    val filter: PartialFunction[Any, Int] = {
      case e: Number => e.intValue
      case null      => null.asInstanceOf[Int]
    }
  }

  implicit val jLongJsonFilter = new JsonFilter[Long] {
    val filter: PartialFunction[Any, Long] = {
      case e: Number => e.longValue
      case null      => null.asInstanceOf[Long]
    }
  }

  implicit val jDoubleJsonFilter = new JsonFilter[Double] {
    val filter: PartialFunction[Any, Double] = {
      case e: Number => e.doubleValue
      case null      => null.asInstanceOf[Double]
    }
  }

  implicit val jFloatJsonFilter = new JsonFilter[Float] {
    val filter: PartialFunction[Any, Float] = {
      case e: Number => e.floatValue
      case null      => null.asInstanceOf[Float]
    }
  }

  implicit val jListJsonFilter = new JsonFilter[Seq[Any]] {
    val filter: PartialFunction[Any, Seq[Any]] = {
      case e: java.util.List[_] => e
      case null                 => null.asInstanceOf[Seq[Any]]
    }
  }

  implicit val jMapJsonFilter = new JsonFilter[Map[String, Any]] {
    val filter: PartialFunction[Any, Map[String, Any]] = {
      case e: java.util.Map[_, _] => e.map { case (key, value) => key.toString -> value }(breakOut)
      case null                   => null.asInstanceOf[Map[String, Any]]
    }
  }

  implicit val anyJsonFilter = new JsonFilter[Any] {
    val filter: PartialFunction[Any, Any] = { case e => e }
  }
}

object JsonFilter extends LowPriorityJsonFilterImplicits {
  def apply[X: JsonFilter] = implicitly[JsonFilter[X]]
}

@implicitNotFound("No member of type class JsonFilter found for type ${X}")
trait JsonFilter[X] {
  def filter: PartialFunction[Any, X]
}
