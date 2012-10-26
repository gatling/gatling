/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.result

class Group(val name: String, val parent: Option[Group]) {
	val groups: List[String] = name :: parent.map(_.groups).getOrElse(Nil)
	val path = RequestPath.path(groups)

	override def equals(obj: Any) =
		obj match {
			case group: Group => group.path == path
			case _ => false
		}

	override val hashCode = path.hashCode()

	override val toString = path
}
object Group {
	def apply(requestName: String, parent: Option[Group] = None) = new Group(name(requestName), parent)

	def name(requestName: String) = requestName.substring(requestName.indexOf("(") + 1, requestName.lastIndexOf(")"))
}

class RequestPath(val name: String, val group: Option[Group]) {
	val path = RequestPath.path(name, group)

	override def equals(obj: Any) =
		obj match {
			case requestPath: RequestPath => requestPath.path == path
			case _ => false
		}

	override val hashCode = path.hashCode()

	override val toString = path
}
object RequestPath {
	val SEPARATOR = " / "

	def apply(requestName: String, group: Option[Group] = None): RequestPath = new RequestPath(requestName, group)

	def path(list: List[String]): String = list.reverse.mkString(SEPARATOR)

	def path(requestName: String, group: Option[Group]): String = path(requestName :: group.map(_.groups).getOrElse(Nil))
}

