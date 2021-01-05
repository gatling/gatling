/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.sse.fsm

import java.nio.CharBuffer

import io.gatling.netty.util.Utf8ByteBufCharsetDecoder

import io.netty.buffer.ByteBuf

object SseStreamDecoder {
  private val BOM = '\uFEFF'
  private val LF = 0x0a
  private val CR = 0x0d

  private val EventHeader = "event:".toCharArray
  private val DataHeader = "data:".toCharArray
  private val IdHeader = "id:".toCharArray
  private val RetryHeader = "retry:".toCharArray
}

class SseStreamDecoder extends Utf8ByteBufCharsetDecoder {

  import SseStreamDecoder._

  private[this] var pendingName: Option[String] = None
  private[this] var pendingData: Option[String] = None
  private[this] var pendingId: Option[String] = None
  private[this] var pendingRetry: Option[Int] = None

  private[this] var pendingEvents = collection.mutable.ArrayBuffer.empty[ServerSentEvent]
  private[this] var firstEvent = true
  private[this] var previousBufferLastCharWasCr = false
  private[this] var position = 0

  private var charArray: Array[Char] = _

  override protected def allocateCharBuffer(l: Int): CharBuffer = {
    charArray = new Array[Char](l)
    CharBuffer.wrap(charArray)
  }

  private def parseLine(lineStart: Int, lineEnd: Int): Unit = {

    val lineLength = lineEnd - lineStart

    def onFieldHeaderMatch(fieldHeader: Array[Char]): Option[String] = {

      val fieldHeaderLength = fieldHeader.length

      if (lineLength < fieldHeaderLength) {
        None

      } else if (
        (0 until fieldHeaderLength).forall { i =>
          charArray(lineStart + i) == fieldHeader(i)
        }
      ) {
        val nextPos = lineStart + fieldHeaderLength
        val valueStart =
          if (charArray(nextPos) == ' ') {
            // white space after colon, trim it
            nextPos + 1
          } else {
            nextPos
          }

        Some(new String(charArray, valueStart, lineEnd - valueStart))

      } else {
        None
      }
    }

    if (lineLength == 0) {
      // empty line, flushing event
      if (pendingName.isDefined || pendingData.isDefined || pendingId.isDefined || pendingRetry.isDefined) {
        // non empty event (eg just a comment)
        pendingEvents += ServerSentEvent(
          name = pendingName,
          data = pendingData,
          id = pendingId,
          retry = pendingRetry
        )

        pendingName = None
        pendingData = None
        pendingId = None
        pendingRetry = None
      }

    } else if (charArray(lineStart) == ':') {
      // comment, skipping

    } else {
      // parse real line
      onFieldHeaderMatch(EventHeader) match {
        case None =>
          onFieldHeaderMatch(DataHeader) match {
            case None =>
              onFieldHeaderMatch(IdHeader) match {
                case None =>
                  onFieldHeaderMatch(RetryHeader) match {
                    case None =>
                    case res  => pendingRetry = res.map(_.toInt)
                  }
                case res => pendingId = res
              }
            case res => pendingData = res
          }
        case res => pendingName = res
      }
    }
  }

  private def parseChars(): Unit = {
    if (firstEvent) {
      if (charArray(position) == BOM) {
        position += 1
      }
      firstEvent = false
    } else if (previousBufferLastCharWasCr && charArray(position) == LF) {
      // last buffer ended with a terminated line
      // but we were actually in the middle of a CRLF pair
      position += 1
    }

    // scanning actual content
    var i = position
    val sbLength = charBuffer.position()

    while (i < sbLength) {
      charArray(i) match {
        case CR =>
          parseLine(position, i)
          if (i < sbLength - 1 && charArray(i + 1) == LF) {
            // skip next LF
            i += 2
          } else {
            i += 1
          }
          position = i

        case LF =>
          parseLine(position, i)
          i += 1
          position = i

        case _ =>
          i += 1
      }
    }
  }

  private def translateCharBuffer(): Unit = {
    val sbLength = charBuffer.position()
    previousBufferLastCharWasCr = charArray(sbLength - 1) == CR
    if (position >= sbLength) {
      // all read, clear
      charBuffer.position(0)
    } else if (position > 0) {
      // partially read, translate
      System.arraycopy(charArray, position, charArray, 0, sbLength - position)
      charBuffer.position(sbLength - position)
    }
    position = 0
  }

  private def flushEvents(): Seq[ServerSentEvent] = {
    val events = pendingEvents.toVector
    pendingEvents.clear()
    events
  }

  def decodeStream(buf: ByteBuf): Seq[ServerSentEvent] = {
    if (buf.isReadable) {
      ensureCapacity(buf.readableBytes)
      if (buf.nioBufferCount == 1) {
        decodePartial(buf.internalNioBuffer(buf.readerIndex, buf.readableBytes), false)
      } else {
        buf.nioBuffers.foreach(decodePartial(_, false))
      }

      parseChars()
      translateCharBuffer()
      flushEvents()
    } else {
      Nil
    }
  }
}
