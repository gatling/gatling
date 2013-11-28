/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.util

import java.io.InputStream

class BytesInputStream(array: Array[Byte]) extends InputStream {

	var offset: Int = _
	var length: Int = array.length
	var position: Int = _
	var mark: Int = _

	override def markSupported = true

	override def reset() {
		position = mark
	}

	override def close() {}

	override def mark(dummy: Int) {
		mark = position
	}

	override def available(): Int = length - position

	override def skip(n: Long): Long = {
		if (n <= length - position) {
			position += n.toInt
			n
		} else {
			val n = length - position
			position = length
			n
		}
	}

	override def read(): Int = {
		if (length == position) -1
		else {
			val oldPosition = position
			position += 1
			array(offset + oldPosition) & 0xFF
		}
	}

	override def read(b: Array[Byte], offset: Int, length: Int): Int = {
		if (length == position) {
			if (length == 0) 0 else -1
		} else {
			val n = math.min(length, length - position)
			System.arraycopy(array, offset + position, b, offset, n)
			position += n
			n
		}
	}
}
