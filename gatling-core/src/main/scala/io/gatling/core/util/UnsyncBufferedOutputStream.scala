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
package io.gatling.core.util

import java.io.OutputStream
import com.typesafe.scalalogging.slf4j.StrictLogging

class UnsyncBufferedOutputStream(os: OutputStream, bufferSize: Int) extends StrictLogging {

	private var bufferPosition = 0
	private val buffer = new Array[Byte](bufferSize)

	def flush() {
		os.write(buffer, 0, bufferPosition)
		bufferPosition = 0
	}

	def write(bytes: Array[Byte]) {
		if (bytes.length + bufferPosition > bufferSize) {
			flush()
		}

		if (bytes.length > bufferSize) {
			// can't write in buffer
			logger.warn(s"Buffer size $bufferSize is not sufficient for message of size ${bytes.length}")
			os.write(bytes)

		} else {
			System.arraycopy(bytes, 0, buffer, bufferPosition, bytes.length)
			bufferPosition += bytes.length
		}
	}
}

