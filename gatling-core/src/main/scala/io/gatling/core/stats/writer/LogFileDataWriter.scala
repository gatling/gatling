/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.core.stats.writer

import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.charset.CharsetEncoder
import java.nio.charset.StandardCharsets.US_ASCII
import java.nio.{ ByteBuffer, CharBuffer }

import io.gatling.commons.stats.assertion.Assertion
import io.gatling.commons.util.StringHelper.EolBytes
import io.gatling.core.config.GatlingFiles.simulationLogDirectory
import io.gatling.commons.util.PathHelper._
import io.gatling.commons.util.{ Clock, StringReplace }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.{ Integers, Longs }

import com.typesafe.scalalogging.StrictLogging

object BufferedFileChannelWriter {

  def apply(runId: String, configuration: GatlingConfiguration): BufferedFileChannelWriter = {
    val encoder = configuration.core.charset.newEncoder
    val simulationLog = simulationLogDirectory(runId)(configuration) / "simulation.log"
    val channel = new RandomAccessFile(simulationLog.toFile, "rw").getChannel
    val bb = ByteBuffer.allocate(configuration.data.file.bufferSize)

    new BufferedFileChannelWriter(channel, encoder, bb)
  }
}

final class BufferedFileChannelWriter(channel: FileChannel, encoder: CharsetEncoder, bb: ByteBuffer) extends AutoCloseable with StrictLogging {

  def flush(): Unit = {
    bb.flip()
    while (bb.hasRemaining) {
      channel.write(bb)
    }
    bb.clear()
  }

  private def ensureCapacity(i: Int): Unit =
    if (bb.remaining < i) {
      flush()
    }

  def writeBytes(bytes: Array[Byte]): Unit = {
    ensureCapacity(bytes.length)
    bb.put(bytes)
  }

  def writeString(string: String): Unit = {

    ensureCapacity(string.length * 4)

    val coderResult = encoder.encode(CharBuffer.wrap(string), bb, false)
    if (coderResult.isOverflow) {
      logger.error("Buffer overflow, you shouldn't be logging that much data. Truncating.")
    }
  }

  def writePositiveLong(l: Long): Unit = {

    val stringSize = Longs.positiveLongStringSize(l)
    ensureCapacity(stringSize)

    Longs.writePositiveLongString(l, stringSize, bb)
  }

  def writePositiveInt(i: Int): Unit = {

    val stringSize = Integers.positiveIntStringSize(i)
    ensureCapacity(stringSize)

    Integers.writePositiveIntString(i, stringSize, bb)
  }

  override def close(): Unit = {
    flush()
    channel.force(true)
  }
}

object DataWriterMessageSerializer {

  val Separator: String = "\t"
  val GroupSeparatorChar = ','
  val SeparatorBytes: Array[Byte] = Separator.getBytes(US_ASCII)

  val SpaceBytes: Array[Byte] = Array(' ')
  val GroupSeparatorBytes: Array[Byte] = GroupSeparatorChar.toString.getBytes(US_ASCII)

  /**
   * Converts whitespace characters that would break the simulation log format into spaces.
   */
  def sanitize(text: String): String =
    StringReplace.replace(text, c => c == '\n' || c == '\r' || c == '\t', ' ')
}

abstract class DataWriterMessageSerializer[T](writer: BufferedFileChannelWriter, header: String) {

  import DataWriterMessageSerializer._

  def writeSeparator(): Unit =
    writer.writeBytes(SeparatorBytes)

  def writeGroupSeparator(): Unit =
    writer.writeBytes(GroupSeparatorBytes)

  def writeSpace(): Unit =
    writer.writeBytes(SpaceBytes)

  def writeEol(): Unit =
    writer.writeBytes(EolBytes)

  def writeGroups(groupHierarchy: List[String]): Unit = {
    var i = groupHierarchy.length
    groupHierarchy.foreach { group =>
      writer.writeString(StringReplace.replace(group, _ == GroupSeparatorChar, ' '))
      i -= 1
      if (i > 0) {
        writeGroupSeparator()
      }
    }
  }

  private val headerBytes = header.getBytes(US_ASCII)

  def writeHeader(): Unit =
    writer.writeBytes(headerBytes)

  def serialize(m: T): Unit = {
    writeHeader()
    writeSeparator()
    serialize0(m)
    writeEol()
  }

  protected def serialize0(m: T): Unit
}

class RunMessageSerializer(writer: BufferedFileChannelWriter) extends DataWriterMessageSerializer[RunMessage](writer, RunRecordHeader.value) {

  override protected def serialize0(runMessage: RunMessage): Unit = {
    import runMessage._
    writer.writeString(simulationClassName)
    writeSeparator()
    writer.writeString(simulationId)
    writeSeparator()
    writer.writePositiveLong(start)
    writeSeparator()
    if (runDescription.isEmpty) {
      writeSpace()
    } else {
      writer.writeString(runDescription)
    }
    writeSeparator()
    writer.writeString(gatlingVersion)
  }
}

class UserMessageSerializer(writer: BufferedFileChannelWriter) extends DataWriterMessageSerializer[UserMessage](writer, UserRecordHeader.value) {

  override protected def serialize0(user: UserMessage): Unit = {
    import user._
    writer.writeString(session.scenario)
    writeSeparator()
    writer.writePositiveLong(session.userId)
    writeSeparator()
    writer.writeString(event.name)
    writeSeparator()
    writer.writePositiveLong(session.startDate)
    writeSeparator()
    writer.writePositiveLong(timestamp)
  }
}

class ResponseMessageSerializer(writer: BufferedFileChannelWriter) extends DataWriterMessageSerializer[ResponseMessage](writer, RequestRecordHeader.value) {

  import DataWriterMessageSerializer._

  override protected def serialize0(response: ResponseMessage): Unit = {
    import response._
    writer.writePositiveLong(userId)
    writeSeparator()
    writeGroups(groupHierarchy)
    writeSeparator()
    writer.writeString(name)
    writeSeparator()
    writer.writePositiveLong(startTimestamp)
    writeSeparator()
    writer.writePositiveLong(endTimestamp)
    writeSeparator()
    writer.writeString(status.name)
    writeSeparator()
    message match {
      case Some(m) => writer.writeString(sanitize(m))
      case _       => writeSpace()
    }
  }
}

class GroupMessageSerializer(writer: BufferedFileChannelWriter) extends DataWriterMessageSerializer[GroupMessage](writer, GroupRecordHeader.value) {

  override protected def serialize0(group: GroupMessage): Unit = {
    import group._
    writer.writePositiveLong(userId)
    writeSeparator()
    writeGroups(groupHierarchy)
    writeSeparator()
    writer.writePositiveLong(startTimestamp)
    writeSeparator()
    writer.writePositiveLong(endTimestamp)
    writeSeparator()
    writer.writePositiveLong(cumulatedResponseTime)
    writeSeparator()
    writer.writeString(status.name)
  }
}

class AssertionSerializer(writer: BufferedFileChannelWriter) extends DataWriterMessageSerializer[Assertion](writer, AssertionRecordHeader.value) {

  override protected def serialize0(assertion: Assertion): Unit = {
    import boopickle.Default._
    import jodd.util.Base64

    val byteBuffer = Pickle.intoBytes(assertion)
    val bytes = new Array[Byte](byteBuffer.remaining)
    byteBuffer.get(bytes)

    writer.writeBytes(Base64.encodeToByte(bytes))
  }
}

class ErrorMessageSerializer(writer: BufferedFileChannelWriter) extends DataWriterMessageSerializer[ErrorMessage](writer, ErrorRecordHeader.value) {

  override protected def serialize0(error: ErrorMessage): Unit = {
    import error._
    writer.writeString(message)
    writeSeparator()
    writer.writePositiveLong(date)
  }
}

case class FileData(
    userMessageSerializer:     UserMessageSerializer,
    responseMessageSerializer: ResponseMessageSerializer,
    groupMessageSerializer:    GroupMessageSerializer,
    errorMessageSerializer:    ErrorMessageSerializer,
    writer:                    BufferedFileChannelWriter
) extends DataWriterData

class LogFileDataWriter(clock: Clock, configuration: GatlingConfiguration) extends DataWriter[FileData] {

  def onInit(init: Init): FileData = {

    import init._

    val writer = BufferedFileChannelWriter(runMessage.runId, configuration)
    system.registerOnTermination(writer.close())

    val assertionSerializer = new AssertionSerializer(writer)
    assertions.foreach(assertion => assertionSerializer.serialize(assertion))
    new RunMessageSerializer(writer).serialize(runMessage)

    FileData(
      new UserMessageSerializer(writer),
      new ResponseMessageSerializer(writer),
      new GroupMessageSerializer(writer),
      new ErrorMessageSerializer(writer),
      writer
    )
  }

  override def onFlush(data: FileData): Unit = {}

  override def onMessage(message: LoadEventMessage, data: FileData): Unit =
    message match {
      case user: UserMessage         => data.userMessageSerializer.serialize(user)
      case group: GroupMessage       => data.groupMessageSerializer.serialize(group)
      case response: ResponseMessage => data.responseMessageSerializer.serialize(response)
      case error: ErrorMessage       => data.errorMessageSerializer.serialize(error)
      case _                         =>
    }

  override def onCrash(cause: String, data: FileData): Unit = {}

  override def onStop(data: FileData): Unit =
    data.writer.close()
}
