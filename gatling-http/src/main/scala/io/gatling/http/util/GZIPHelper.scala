/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.http.util

import java.io.{ ByteArrayOutputStream, InputStream }

import org.apache.commons.io.IOUtils

import com.jcraft.jzlib.GZIPOutputStream

import io.gatling.core.util.IOHelper.withCloseable

object GZIPHelper {

	def gzip(string: String): Array[Byte] = gzip(string.getBytes)

	def gzip(bytes: Array[Byte]): Array[Byte] = {
		val bytesOut = new ByteArrayOutputStream

		withCloseable(new GZIPOutputStream(bytesOut)) {
			_.write(bytes)
		}

		bytesOut.toByteArray
	}

	def gzip(in: InputStream): Array[Byte] = {

		val bytesOut = new ByteArrayOutputStream

		withCloseable(new GZIPOutputStream(bytesOut)) {
			IOUtils.copy(in, _)
		}

		bytesOut.toByteArray
	}
}