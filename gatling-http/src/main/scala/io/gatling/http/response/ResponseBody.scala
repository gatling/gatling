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
package io.gatling.http.response

import java.io.{ ByteArrayInputStream, InputStream }
import java.nio.charset.Charset

import scala.annotation.switch

import org.jboss.netty.buffer.{ ChannelBuffer, ChannelBufferInputStream, ChannelBuffers }

import io.gatling.core.util.UnsyncByteArrayInputStream

sealed trait ResponseBodyUsage
case object StringResponseBodyUsage extends ResponseBodyUsage
case object ByteArrayResponseBodyUsage extends ResponseBodyUsage
case object InputStreamResponseBodyUsage extends ResponseBodyUsage

trait ResponseBodyUsageStrategy {
	def bodyUsage(bodyLength: Int): ResponseBodyUsage
}

object StringResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
	def bodyUsage(bodyLength: Int) = StringResponseBodyUsage
}

object ByteArrayResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
	def bodyUsage(bodyLength: Int) = ByteArrayResponseBodyUsage
}

object InputStreamResponseBodyUsageStrategy extends ResponseBodyUsageStrategy {
	def bodyUsage(bodyLength: Int) = InputStreamResponseBodyUsage
}

object ResponseBody {

	val emptyBytes = new Array[Byte](0)

	private def getBytes(buffer: ChannelBuffer, start: Int, length: Int): Array[Byte] = {
		val array = new Array[Byte](length)
		buffer.getBytes(start, array)
		array
	}

	def chunks2Bytes(chunks: Seq[ChannelBuffer]): Array[Byte] = (chunks.size: @switch) match {

		case 0 => emptyBytes

		case 1 =>
			val headChunk = chunks.head
			val readableBytes = headChunk.readableBytes
			val readerIndex = headChunk.readerIndex

			if (headChunk.hasArray && headChunk.arrayOffset == 0 && readerIndex == 0 && readableBytes == headChunk.array.length)
				headChunk.array
			else
				getBytes(headChunk, readerIndex, readableBytes)

		case _ =>
			val composite = ChannelBuffers.wrappedBuffer(chunks: _*)
			getBytes(composite, composite.readerIndex, composite.readableBytes)
	}

	def chunks2String(chunks: Seq[ChannelBuffer], charset: Charset): String = (chunks.size: @switch) match {

		case 0 => ""

		case 1 => chunks.head.toString(charset)

		case _ => ChannelBuffers.wrappedBuffer(chunks: _*).toString(charset)
	}
}

sealed trait ResponseBody {
	def string(): String
	def bytes(): Array[Byte]
	def stream(): InputStream
}

object StringResponseBody {

	def apply(chunks: Seq[ChannelBuffer], charset: Charset) = {
		val string = ResponseBody.chunks2String(chunks, charset)
		new StringResponseBody(string, charset)
	}
}

case class StringResponseBody(string: String, charset: Charset) extends ResponseBody {

	lazy val bytes = string.getBytes(charset)
	def stream() = new ByteArrayInputStream(bytes)
}

object ByteArrayResponseBody {

	def apply(chunks: Seq[ChannelBuffer], charset: Charset) = {
		val bytes = ResponseBody.chunks2Bytes(chunks)
		new ByteArrayResponseBody(bytes, charset)
	}
}

case class ByteArrayResponseBody(bytes: Array[Byte], charset: Charset) extends ResponseBody {

	def stream() = new UnsyncByteArrayInputStream(bytes)
	lazy val string = new String(bytes, charset)
}

case class InputStreamResponseBody(chunks: Seq[ChannelBuffer], charset: Charset) extends ResponseBody {

	var bytesLoaded = false

	def stream() = (chunks.size: @switch) match {

		case 0 => new UnsyncByteArrayInputStream(ResponseBody.emptyBytes)

		case 1 =>
			new ChannelBufferInputStream(chunks.head.duplicate)

		case _ =>
			val composite = ChannelBuffers.wrappedBuffer(chunks.map(_.duplicate): _*)
			new ChannelBufferInputStream(composite)
	}

	lazy val bytes = {
		bytesLoaded = true
		ResponseBody.chunks2Bytes(chunks)
	}

	lazy val string = {
		if (bytesLoaded)
			new String(bytes, charset)
		else
			ResponseBody.chunks2String(chunks, charset)
	}
}
