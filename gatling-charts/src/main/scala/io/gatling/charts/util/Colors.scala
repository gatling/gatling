/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.charts.util

sealed trait Color { def code: String }

object Colors {

  implicit def color2String(c: Color) = c.code

  case object Blue extends Color { val code = "#4572A7" }
  case object Green extends Color { val code = "#A0B228" }
  case object Yellow extends Color { val code = "#FFDD00" }
  case object Orange extends Color { val code = "#FF9D00" }
  case object Red extends Color { val code = "#FF0000" }
  case object Cyan extends Color { val code = "#00D5FF" }
  case object Lime extends Color { val code = "#00FF00" }
  case object Purple extends Color { val code = "#9D00FF" }
  case object Pink extends Color { val code = "#FF00E1" }
  case object Black extends Color { val code = "#000000" }
  case object LightGrey extends Color { val code = "#CFC9C6" }
  case object LightBlue extends Color { val code = "#AECAEB" }
  case object LightOrange extends Color { val code = "#FFD085" }
  case object LightRed extends Color { val code = "#FF9C9C" }
  case object LightLime extends Color { val code = "#9EFF9E" }
  case object LightPurple extends Color { val code = "#CF82FF" }
  case object LightPink extends Color { val code = "#FF82F0" }
  case object TranslucidRed extends Color { val code = "rgba(255, 0, 0, .2)" }
  case object TranslucidBlue extends Color { val code = "rgba(69, 114, 167, .2)" }
  case object DarkGrey extends Color { val code = "#92918C" }
  case object DarkOrange extends Color { val code = "#E37400" }
}
