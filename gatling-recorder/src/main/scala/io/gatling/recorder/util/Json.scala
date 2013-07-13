/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.recorder.util

import scala.language.dynamics

import net.minidev.json.{ JSONArray, JSONValue, JSONObject }

object Json {
	def parseJson(s: String) = new Json(JSONValue.parse(s))

	implicit def JsonToString(s: Json) = s.toString
	implicit def JsonToInt(s: Json) = s.toInt
	implicit def JsonToDouble(s: Json) = s.toDouble
}

class JsonException extends Exception

class JsonIterator(i: java.util.Iterator[Object]) extends Iterator[Json] {
	def hasNext = i.hasNext
	def next = new Json(i.next)
}

class Json(o: Object) extends Seq[Json] with Dynamic {

	override def toString: String = o.toString

	def toInt: Int = o match {
		case i: Integer => i
		case _ => throw new JsonException
	}

	def toDouble: Double = o match {
		case d: java.lang.Double => d
		case f: java.lang.Float => f.toDouble
		case _ => throw new JsonException
	}

	def apply(key: String): Json = o match {
		case m: JSONObject => new Json(m.get(key))
		case _ => throw new JsonException
	}

	def apply(idx: Int): Json = o match {
		case a: JSONArray => new Json(a.get(idx))
		case _ => throw new JsonException
	}

	def length: Int = o match {
		case a: JSONArray => a.size
		case m: JSONObject => m.size
		case _ => throw new JsonException
	}

	def iterator: Iterator[Json] = o match {
		case a: JSONArray => new JsonIterator(a.iterator)
		case _ => Iterator.empty
	}

	def selectDynamic(name: String): Json = apply(name)

	def applyDynamic(name: String)(arg: Any) = {
		arg match {
			case s: String => apply(name)(s)
			case n: Int => apply(name)(n)
			case u: Unit => apply(name)
		}
	}
}