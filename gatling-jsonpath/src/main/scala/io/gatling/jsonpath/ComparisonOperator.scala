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

package io.gatling.jsonpath

import com.fasterxml.jackson.core.JsonParser.NumberType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType._

/**
 * Originally contributed by Nicolas Rémond.
 */
sealed trait ComparisonOperator extends Product with Serializable {
  def apply(lhs: JsonNode, rhs: JsonNode): Boolean
}

// Comparison operators
sealed trait ComparisonWithOrderingOperator extends ComparisonOperator {

  protected def compare[T: Ordering](lhs: T, rhs: T): Boolean

  private def compareNumbers(lhs: JsonNode, rhs: JsonNode): Boolean =
    lhs.numberType match {
      case NumberType.INT =>
        rhs.numberType match {
          case NumberType.INT                       => compare(lhs.intValue, rhs.intValue)
          case NumberType.LONG                      => compare(lhs.longValue, rhs.longValue)
          case NumberType.DOUBLE | NumberType.FLOAT => compare(lhs.doubleValue, rhs.doubleValue)
          case NumberType.BIG_INTEGER               => compare(lhs.bigIntegerValue, rhs.bigIntegerValue)
          case NumberType.BIG_DECIMAL               => compare(lhs.decimalValue, rhs.decimalValue)
        }

      case NumberType.LONG =>
        rhs.numberType match {
          case NumberType.INT | NumberType.LONG     => compare(lhs.longValue, rhs.longValue)
          case NumberType.DOUBLE | NumberType.FLOAT => compare(lhs.doubleValue, rhs.doubleValue)
          case NumberType.BIG_INTEGER               => compare(lhs.bigIntegerValue, rhs.bigIntegerValue)
          case NumberType.BIG_DECIMAL               => compare(lhs.decimalValue, rhs.decimalValue)
        }

      case NumberType.FLOAT =>
        rhs.numberType match {
          case NumberType.INT | NumberType.LONG | NumberType.DOUBLE => compare(lhs.doubleValue, rhs.doubleValue)
          case NumberType.FLOAT                                     => compare(lhs.floatValue, rhs.floatValue)
          case NumberType.BIG_INTEGER | NumberType.BIG_DECIMAL      => compare(lhs.decimalValue, rhs.decimalValue)
        }

      case NumberType.DOUBLE =>
        rhs.numberType match {
          case NumberType.INT | NumberType.LONG | NumberType.DOUBLE | NumberType.FLOAT => compare(lhs.doubleValue, rhs.doubleValue)
          case NumberType.BIG_INTEGER | NumberType.BIG_DECIMAL                         => compare(lhs.decimalValue, rhs.decimalValue)
        }

      case NumberType.BIG_INTEGER =>
        rhs.numberType match {
          case NumberType.INT | NumberType.LONG | NumberType.BIG_INTEGER     => compare(lhs.bigIntegerValue, rhs.bigIntegerValue)
          case NumberType.DOUBLE | NumberType.FLOAT | NumberType.BIG_DECIMAL => compare(lhs.decimalValue, rhs.decimalValue)
        }

      case NumberType.BIG_DECIMAL => compare(lhs.decimalValue, rhs.decimalValue)

      case _ => false
    }

  def apply(lhs: JsonNode, rhs: JsonNode): Boolean =
    lhs.getNodeType match {
      case STRING  => rhs.getNodeType == STRING && compare(lhs.textValue, rhs.textValue)
      case BOOLEAN => rhs.getNodeType == BOOLEAN && compare(lhs.booleanValue, rhs.booleanValue)
      case NUMBER  => rhs.getNodeType == NUMBER && compareNumbers(lhs, rhs)
      case _       => false
    }
}

case object EqWithOrderingOperator extends ComparisonWithOrderingOperator {
  protected def compare[T: Ordering](lhs: T, rhs: T): Boolean = Ordering[T].equiv(lhs, rhs)
}

case object EqOperator extends ComparisonOperator {
  override def apply(lhs: JsonNode, rhs: JsonNode): Boolean =
    (lhs.getNodeType == NULL && rhs.getNodeType == NULL) || EqWithOrderingOperator(lhs, rhs)
}

case object NotEqOperator extends ComparisonOperator {
  override def apply(lhs: JsonNode, rhs: JsonNode): Boolean = !EqOperator(lhs, rhs)
}

case object LessOperator extends ComparisonWithOrderingOperator {
  override protected def compare[T: Ordering](lhs: T, rhs: T): Boolean = Ordering[T].lt(lhs, rhs)
}

case object GreaterOperator extends ComparisonWithOrderingOperator {
  override protected def compare[T: Ordering](lhs: T, rhs: T): Boolean = Ordering[T].gt(lhs, rhs)
}

case object LessOrEqOperator extends ComparisonWithOrderingOperator {
  override protected def compare[T: Ordering](lhs: T, rhs: T): Boolean = Ordering[T].lteq(lhs, rhs)
}

case object GreaterOrEqOperator extends ComparisonWithOrderingOperator {
  override protected def compare[T: Ordering](lhs: T, rhs: T): Boolean = Ordering[T].gteq(lhs, rhs)
}

// Binary boolean operators
sealed trait BinaryBooleanOperator extends Product with Serializable {
  def apply(lhs: Boolean, rhs: Boolean): Boolean
}

case object AndOperator extends BinaryBooleanOperator {
  override def apply(lhs: Boolean, rhs: Boolean): Boolean = lhs && rhs
}

case object OrOperator extends BinaryBooleanOperator {
  override def apply(lhs: Boolean, rhs: Boolean): Boolean = lhs || rhs
}
