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
package io.gatling.metrics

import scala.collection.mutable

object GraphitePath {
  private val sanitizeStringMemo = mutable.Map.empty[String, String]
  def sanitizeString(s: String) = sanitizeStringMemo.getOrElseUpdate(s, s.replace(' ', '_').replace('.', '-').replace('\\', '-'))

  def graphitePath(root: String) = new GraphitePath(List(sanitizeString(root)))
  def graphitePath(path: List[String]) = new GraphitePath(path.map(sanitizeString))
}

case class GraphitePath private (val path: List[String]) {
  import GraphitePath.sanitizeString
  def /(subPath: String) = new GraphitePath(sanitizeString(subPath) :: path)
  def pathKey = path.reverse.mkString(".")
  def pathKeyWithPrefix(prefix: String) = path.reverse.mkString(prefix, ".", "")
}
