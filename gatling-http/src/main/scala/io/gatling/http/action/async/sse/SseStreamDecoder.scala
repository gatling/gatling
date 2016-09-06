/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.http.action.async.sse

import io.netty.buffer.{ ByteBuf, ByteBufProcessor }
import org.asynchttpclient.netty.util.Utf8Decoder

object SseStreamDecoder {
  private val BOM = '\uFEFF'
  private val LF = 0x0A
  private val CR = 0x0D

  val EventHeader = "event:".toCharArray
  val DataHeader = "data:".toCharArray
  val IdHeader = "id:".toCharArray
  val RetryHeader = "retry:".toCharArray
}

class SseStreamDecoder extends Utf8Decoder {

  import SseStreamDecoder._

  private[this] var pendingName: Option[String] = None
  private[this] var pendingData: Option[String] = None
  private[this] var pendingId: Option[String] = None
  private[this] var pendingRetry: Option[Int] = None

  private[this] var pendingEvents = collection.mutable.ArrayBuffer.empty[ServerSentEvent]
  private[this] var firstEvent = true
  private[this] var previousBufferLastCharWasCr = false
  private[this] var position = 0

  private def decodeBytes(buf: ByteBuf): Unit =
    buf.forEachByte(
      new ByteBufProcessor {
        override def process(b: Byte): Boolean = {
          write(b)
          true
        }
      }
    )

  private def parseLine(lineStart: Int, lineEnd: Int): Unit = {

    val lineLength = lineEnd - lineStart

      def onFieldHeaderMatch(fieldHeader: Array[Char]): Option[String] = {

        val fieldHeaderLength = fieldHeader.length

        if (lineLength < fieldHeaderLength) {
          None

        } else if ((0 until fieldHeaderLength).forall { i => sb.charAt(lineStart + i) == fieldHeader(i) }) {
          val nextPos = lineStart + fieldHeaderLength
          val valueStart =
            if (sb.charAt(nextPos) == ' ') {
              // white space after colon, trim it
              nextPos + 1
            } else {
              nextPos
            }

          Some(sb.substring(valueStart, lineEnd))

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

    } else if (sb.charAt(lineStart) == ':') {
      // comment, skipping

    } else {
      // parse real line
      onFieldHeaderMatch(EventHeader) match {
        case None => onFieldHeaderMatch(DataHeader) match {
          case None => onFieldHeaderMatch(IdHeader) match {
            case None => onFieldHeaderMatch(RetryHeader) match {
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
      if (sb.charAt(position) == BOM) {
        position += 1
      }
      firstEvent = false
    } else if (previousBufferLastCharWasCr && sb.charAt(position) == LF) {
      // last buffer ended with a terminated line
      // but we were actually in the middle of a CRLF pair
      position += 1
    }

    // scanning actual content
    var i = position
    val sbLength = sb.length
    while (i < sbLength) {
      sb.charAt(i) match {
        case CR =>
          parseLine(position, i)
          if (i < sbLength - 1 && sb.charAt(i + 1) == LF) {
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
    val sbLength = sb.length
    previousBufferLastCharWasCr = sb.charAt(sbLength - 1) == CR
    if (position > sbLength) {
      // all read, clear
      sb.setLength(0)
      position = 0

    } else if (position > 0) {
      // partially read, translate
      for (i <- position until sbLength) {
        sb.setCharAt(i - position, sb.charAt(i))
      }
      sb.setLength(sbLength - position)
      position = 0
    }
  }

  private def flushEvents(): Seq[ServerSentEvent] = {
    val events = pendingEvents.toVector
    pendingEvents.clear()
    events
  }

  def decode(buf: ByteBuf): Seq[ServerSentEvent] = {
    if (buf.readableBytes == 0) {
      Nil
    } else {
      decodeBytes(buf)
      parseChars()
      translateCharBuffer()
      flushEvents()
    }
  }
}
