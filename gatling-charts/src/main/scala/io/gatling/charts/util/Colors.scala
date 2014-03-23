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

object Colors extends Enumeration {
  type Colors = Value
  val BLUE = Value("#4572A7")
  val GREEN = Value("#A0B228")
  val YELLOW = Value("#FFDD00")
  val ORANGE = Value("#FF9D00")
  val RED = Value("#FF0000")
  val CYAN = Value("#00D5FF")
  val LIME = Value("#00FF00")
  val PURPLE = Value("#9D00FF")
  val PINK = Value("#FF00E1")
  val LIGHT_BLUE = Value("#AECAEB")
  val LIGHT_ORANGE = Value("#FFD085")
  val LIGHT_RED = Value("#FF9C9C")
  val LIGHT_LIME = Value("#9EFF9E")
  val LIGHT_PURPLE = Value("#CF82FF")
  val LIGHT_PINK = Value("#FF82F0")
  val TRANSLUCID_RED = Value("rgba(255, 0, 0, .2)")
  val TRANSLUCID_BLUE = Value("rgba(69, 114, 167, .2)")

  implicit def color2String(color: Colors) = color.toString
}