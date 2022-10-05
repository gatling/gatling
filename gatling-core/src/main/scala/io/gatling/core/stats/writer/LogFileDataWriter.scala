/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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
import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.channels.FileChannel
import java.nio.charset.CharsetEncoder
import java.nio.charset.StandardCharsets.US_ASCII
import java.util.Base64

import io.gatling.commons.stats.assertion.Assertion
import io.gatling.commons.util.Clock
import io.gatling.commons.util.StringHelper._
import io.gatling.commons.util.StringHelper.EolBytes
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingFiles.simulationLogDirectory
import io.gatling.core.stats.message.MessageEvent
import io.gatling.core.util.{ Integers, Longs }

import com.typesafe.scalalogging.StrictLogging

object BufferedFileChannelWriter {

  def apply(runId: String, configuration: GatlingConfiguration): BufferedFileChannelWriter = {
    val encoder = configuration.core.charset.newEncoder
    val simulationLog = simulationLogDirectory(runId, create = true, configuration).resolve("simulation.log")
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

  override def close(): Unit =
    try {
      flush()
      channel.force(true)
    } finally {
      channel.close()
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
    text.replaceIf(c => c == '\n' || c == '\r' || c == '\t', ' ')
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
      writer.writeString(group.replaceIf(_ == GroupSeparatorChar, ' '))
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

class RunMessageSerializer(writer: BufferedFileChannelWriter) extends DataWriterMessageSerializer[RunMessage](writer, RecordHeader.Run.value) {

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

class UserStartMessageSerializer(writer: BufferedFileChannelWriter)
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.UserStart](writer, RecordHeader.User.value) {

  override protected def serialize0(user: DataWriterMessage.LoadEvent.UserStart): Unit = {
    import user._
    writer.writeString(scenario)
    writeSeparator()
    writer.writeString(MessageEvent.Start.name)
    writeSeparator()
    writer.writePositiveLong(timestamp)
  }
}

class UserEndMessageSerializer(writer: BufferedFileChannelWriter)
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.UserEnd](writer, RecordHeader.User.value) {

  override protected def serialize0(user: DataWriterMessage.LoadEvent.UserEnd): Unit = {
    import user._
    writer.writeString(scenario)
    writeSeparator()
    writer.writeString(MessageEvent.End.name)
    writeSeparator()
    writer.writePositiveLong(timestamp)
  }
}

class ResponseMessageSerializer(writer: BufferedFileChannelWriter)
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.Response](writer, RecordHeader.Request.value) {

  import DataWriterMessageSerializer._

  override protected def serialize0(response: DataWriterMessage.LoadEvent.Response): Unit = {
    import response._
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

class GroupMessageSerializer(writer: BufferedFileChannelWriter)
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.Group](writer, RecordHeader.Group.value) {

  override protected def serialize0(group: DataWriterMessage.LoadEvent.Group): Unit = {
    import group._
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

class AssertionSerializer(writer: BufferedFileChannelWriter) extends DataWriterMessageSerializer[Assertion](writer, RecordHeader.Assertion.value) {

  import io.gatling.commons.stats.assertion.AssertionPicklers._

  override protected def serialize0(assertion: Assertion): Unit = {
    import boopickle.Default._

    val byteBuffer = Pickle.intoBytes(assertion)
    val bytes = new Array[Byte](byteBuffer.remaining)
    byteBuffer.get(bytes)

    writer.writeBytes(Base64.getEncoder.encode(bytes))
  }
}

class ErrorMessageSerializer(writer: BufferedFileChannelWriter)
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.Error](writer, RecordHeader.Error.value) {

  override protected def serialize0(error: DataWriterMessage.LoadEvent.Error): Unit = {
    import error._
    writer.writeString(message)
    writeSeparator()
    writer.writePositiveLong(date)
  }
}

final class FileData(
    val userStartMessageSerializer: UserStartMessageSerializer,
    val userEndMessageSerializer: UserEndMessageSerializer,
    val responseMessageSerializer: ResponseMessageSerializer,
    val groupMessageSerializer: GroupMessageSerializer,
    val errorMessageSerializer: ErrorMessageSerializer,
    val writer: BufferedFileChannelWriter
) extends DataWriterData

class LogFileDataWriter(clock: Clock, configuration: GatlingConfiguration) extends DataWriter[FileData] {

  def onInit(init: DataWriterMessage.Init): FileData = {

    import init._

    val writer = BufferedFileChannelWriter(runMessage.runId, configuration)
    val assertionSerializer = new AssertionSerializer(writer)
    assertions.foreach(assertion => assertionSerializer.serialize(assertion))
    new RunMessageSerializer(writer).serialize(runMessage)

    new FileData(
      new UserStartMessageSerializer(writer),
      new UserEndMessageSerializer(writer),
      new ResponseMessageSerializer(writer),
      new GroupMessageSerializer(writer),
      new ErrorMessageSerializer(writer),
      writer
    )
  }

  override def onFlush(data: FileData): Unit = {}

  override def onMessage(message: DataWriterMessage.LoadEvent, data: FileData): Unit =
    message match {
      case user: DataWriterMessage.LoadEvent.UserStart    => data.userStartMessageSerializer.serialize(user)
      case user: DataWriterMessage.LoadEvent.UserEnd      => data.userEndMessageSerializer.serialize(user)
      case group: DataWriterMessage.LoadEvent.Group       => data.groupMessageSerializer.serialize(group)
      case response: DataWriterMessage.LoadEvent.Response => data.responseMessageSerializer.serialize(response)
      case error: DataWriterMessage.LoadEvent.Error       => data.errorMessageSerializer.serialize(error)
      case _                                              =>
    }

  override def onCrash(cause: String, data: FileData): Unit = {}

  override def onStop(data: FileData): Unit =
    data.writer.close()
}
