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
package io.gatling.recorder.util

import java.io.InputStream
import java.util.{ List => JList, Map => JMap }

import scala.language.dynamics

import com.fasterxml.jackson.databind.ObjectMapper

object Json {
	val objectMapper = new ObjectMapper

	def parseJson(is: InputStream) = new Json(objectMapper.readValue(is, classOf[Object]))

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
		case m: JMap[_, _] => new Json(m.get(key).asInstanceOf[Object])
		case _ => throw new JsonException
	}

	def apply(idx: Int): Json = o match {
		case a: JList[_] => new Json(a.get(idx).asInstanceOf[Object])
		case _ => throw new JsonException
	}

	def length: Int = o match {
		case a: JList[_] => a.size
		case m: JMap[_, _] => m.size
		case _ => throw new JsonException
	}

	def iterator: Iterator[Json] = o match {
		case a: JList[_] => new JsonIterator(a.asInstanceOf[JList[Object]].iterator)
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