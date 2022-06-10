/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

private[gatling] sealed abstract class Color(val code: String) extends Product with Serializable {
  override def toString: String = code
}

private[gatling] object Color {

  object Requests {
    val All: Color = Blue
    val Ok: Color = Green
    val Ko: Color = Red
    val Fine: Color = Yellow
    val Poor: Color = Orange

    val Percentiles: List[Color] = List(
      Min,
      P25,
      P50,
      P75,
      P80,
      P85,
      P90,
      P95,
      P99,
      Max,
      Orange
    )

    private case object Min extends Color("#c4fd90")
    private case object P25 extends Color("#7ff77f")
    private case object P50 extends Color("#6ff2ad")
    private case object P75 extends Color("#61ede6")
    private case object P80 extends Color("#58c7e0")
    private case object P85 extends Color("#4ea1d4")
    private case object P90 extends Color("#487ad9")
    private case object P95 extends Color("#3f52cc")
    private case object P99 extends Color("#7335dc")
    private case object Max extends Color("#c73905")
  }

  object Users {
    val All: Color = Orange
    val Base: List[Color] = List(Blue, Green, Red, Yellow, Cyan, Lime, Purple, Pink, LightBlue, LightOrange, LightRed, LightLime, LightPurple, LightPink)
  }

  private case object Blue extends Color("#5E7BE2")
  private case object Green extends Color("#68b65c") // #24C223 on Cloud
  private case object Yellow extends Color("#FFDD00")
  private case object Orange extends Color("#FFA900")
  private case object Red extends Color("#f15b4f") // #E61016 on Cloud
  private case object Cyan extends Color("#00D5FF")
  private case object Lime extends Color("#00FF00")
  private case object Purple extends Color("#9D00FF")
  private case object Pink extends Color("#FF00E1")
  private case object LightBlue extends Color("#AECAEB")
  private case object LightOrange extends Color("#FFD085")
  private case object LightRed extends Color("#FF9C9C")
  private case object LightLime extends Color("#9EFF9E")
  private case object LightPurple extends Color("#CF82FF")
  private case object LightPink extends Color("#FF82F0")

  object RangeSelector {
    case object Border extends Color("#000000")
    case object Fill extends Color("#CFC9C6")
    case object Hover extends Color("#92918C")
    val Selected: Color = Orange
  }
}
