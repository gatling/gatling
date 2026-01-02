/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.charts.component

import java.text.{ DecimalFormat, DecimalFormatSymbols }
import java.util.Locale

private object Component {
  private val Formatter = new DecimalFormat("###.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
}

private[gatling] abstract class Component {
  def html: String

  def js: String

  def jsFiles: Seq[String]

  private def format[T: Numeric](value: Option[T], default: String): String =
    value match {
      case Some(v) => Component.Formatter.format(implicitly[Numeric[T]].toDouble(v))
      case _       => default
    }

  protected def styleCount[T: Numeric](value: Option[T]): String =
    format(value, "0")

  protected def styleStatistic[T: Numeric](value: Option[T]): String =
    format(value, "-")
}
