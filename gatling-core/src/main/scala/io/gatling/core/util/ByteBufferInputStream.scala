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
package io.gatling.core.util

import java.io.InputStream
import java.nio.ByteBuffer

class ByteBufferInputStream(byteBuffer: ByteBuffer) extends InputStream {

	def read: Int = if (!byteBuffer.hasRemaining) -1 else byteBuffer.get

	override def read(bytes: Array[Byte], offset: Int, length: Int) = {
		val count = math.min(byteBuffer.remaining, length);
		if (count == 0)
			-1
		else {
			byteBuffer.get(bytes, offset, length)
			count
		}
	}

	override def available: Int = byteBuffer.remaining
}