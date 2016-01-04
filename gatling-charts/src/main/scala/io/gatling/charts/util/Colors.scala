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
package io.gatling.charts.util

private[gatling] sealed abstract class Color(val code: String)

private[gatling] object Colors {

  implicit def color2String(c: Color): String = c.code

  case object Blue extends Color("#4572A7")
  case object Green extends Color("#A0B228")
  case object Yellow extends Color("#FFDD00")
  case object Orange extends Color("#FF9D00")
  case object Red extends Color("#FF0000")
  case object Cyan extends Color("#00D5FF")
  case object Lime extends Color("#00FF00")
  case object Purple extends Color("#9D00FF")
  case object Pink extends Color("#FF00E1")
  case object Black extends Color("#000000")
  case object LightGrey extends Color("#CFC9C6")
  case object LightBlue extends Color("#AECAEB")
  case object LightOrange extends Color("#FFD085")
  case object LightRed extends Color("#FF9C9C")
  case object LightLime extends Color("#9EFF9E")
  case object LightPurple extends Color("#CF82FF")
  case object LightPink extends Color("#FF82F0")
  case object TranslucidRed extends Color("rgba(255, 0, 0, .2)")
  case object TranslucidBlue extends Color("rgba(69, 114, 167, .2)")
  case object DarkGrey extends Color("#92918C")
  case object DarkOrange extends Color("#E37400")
}
