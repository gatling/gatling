/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import java.{ lang => jl, util => ju }
import java.io.RandomAccessFile
import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path

import scala.jdk.CollectionConverters.MapHasAsScala

import io.gatling.commons.stats.OK
import io.gatling.commons.stats.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingFiles.simulationLogDirectory

import com.typesafe.scalalogging.StrictLogging

private object BufferedFileChannelWriter {
  private val Utf8MaxBytesPerChar = 4
}

private[writer] final class BufferedFileChannelWriter(channel: FileChannel, bb: ByteBuffer) extends AutoCloseable with StrictLogging {

  private val encoder = UTF_8.newEncoder

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

  def writeByte(byte: Byte): Unit = {
    ensureCapacity(jl.Byte.BYTES)
    bb.put(byte)
  }

  def writeBoolean(boolean: Boolean): Unit =
    writeByte(if (boolean) 1 else 0)

  def writeBytes(bytes: Array[Byte]): Unit = {
    ensureCapacity(jl.Integer.BYTES + bytes.length)
    bb.putInt(bytes.length)
    bb.put(bytes)
  }

  def writeByteBuffer(src: ByteBuffer): Unit = {
    ensureCapacity(jl.Integer.BYTES + src.remaining)
    bb.putInt(src.remaining)
    bb.put(src)
  }

  def writeString(string: String): Unit = {
    ensureCapacity(jl.Integer.BYTES + string.length * BufferedFileChannelWriter.Utf8MaxBytesPerChar)
    val lenPosition = bb.position()
    val stringStartPosition = lenPosition + jl.Integer.BYTES

    // leave room for stringLen
    bb.position(stringStartPosition)

    val coderResult = encoder.encode(CharBuffer.wrap(string), bb, false)
    if (coderResult.isOverflow) {
      logger.error("Buffer overflow, you shouldn't be logging that much data. Truncating.")
    }

    val stringLen = bb.position() - stringStartPosition
    bb.putInt(lenPosition, stringLen)
  }

  def writeInt(l: Int): Unit = {
    ensureCapacity(jl.Integer.BYTES)
    bb.putInt(l)
  }

  def writeLong(l: Long): Unit = {
    ensureCapacity(jl.Long.BYTES)
    bb.putLong(l)
  }

  override def close(): Unit =
    try {
      flush()
      channel.force(true)
    } finally {
      channel.close()
    }
}

abstract class DataWriterMessageSerializer[T](writer: BufferedFileChannelWriter, header: Byte) {

  def writeGroups(groupHierarchy: List[String]): Unit = {
    writer.writeInt(groupHierarchy.length)
    groupHierarchy.foreach(writer.writeString)
  }

  def serialize(m: T): Unit = {
    writer.writeByte(header)
    serialize0(m)
  }

  protected def serialize0(m: T): Unit
}

final class RunMessageSerializer(writer: BufferedFileChannelWriter)
    extends DataWriterMessageSerializer[(RunMessage, Seq[Assertion], ju.Map[String, Int])](writer, RecordHeader.Run.value) {
  override protected def serialize0(init: (RunMessage, Seq[Assertion], ju.Map[String, Int])): Unit = {
    val (runMessage, assertions, scenarios) = init
    import runMessage._
    writer.writeString(gatlingVersion)
    writer.writeString(simulationClassName)
    writer.writeLong(start)
    writer.writeString(runDescription)
    writer.writeInt(scenarios.size)
    scenarios.asScala.toSeq.sortBy(_._2).map(_._1).foreach(writer.writeString)
    writer.writeInt(assertions.size)
    assertions.foreach { assertion =>
      import boopickle.Default._
      val byteBuffer = Pickle.intoBytes(assertion)
      writer.writeByteBuffer(byteBuffer)
    }
  }
}

final class UserStartMessageSerializer(writer: BufferedFileChannelWriter, start: Long, scenarios: ju.Map[String, Int])
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.UserStart](writer, RecordHeader.User.value) {
  override protected def serialize0(user: DataWriterMessage.LoadEvent.UserStart): Unit = {
    import user._
    writer.writeInt(scenarios.get(scenario))
    writer.writeBoolean(true)
    writer.writeInt((timestamp - start).toInt)
  }
}

final class UserEndMessageSerializer(writer: BufferedFileChannelWriter, start: Long, scenarios: ju.Map[String, Int])
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.UserEnd](writer, RecordHeader.User.value) {
  override protected def serialize0(user: DataWriterMessage.LoadEvent.UserEnd): Unit = {
    import user._
    writer.writeInt(scenarios.get(scenario))
    writer.writeBoolean(false)
    writer.writeInt((timestamp - start).toInt)
  }
}

final class ResponseMessageSerializer(writer: BufferedFileChannelWriter, start: Long)
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.Response](writer, RecordHeader.Request.value) {

  override protected def serialize0(response: DataWriterMessage.LoadEvent.Response): Unit = {
    import response._
    writeGroups(groupHierarchy)
    writer.writeString(name)
    writer.writeInt((startTimestamp - start).toInt)
    writer.writeInt((endTimestamp - start).toInt)
    writer.writeBoolean(status == OK)
    writer.writeString(message.getOrElse(""))
  }
}

class GroupMessageSerializer(writer: BufferedFileChannelWriter, start: Long)
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.Group](writer, RecordHeader.Group.value) {
  override protected def serialize0(group: DataWriterMessage.LoadEvent.Group): Unit = {
    import group._
    writeGroups(groupHierarchy)
    writer.writeInt((startTimestamp - start).toInt)
    writer.writeInt((endTimestamp - start).toInt)
    writer.writeInt(cumulatedResponseTime)
    writer.writeBoolean(status == OK)
  }
}

class ErrorMessageSerializer(writer: BufferedFileChannelWriter, start: Long)
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.Error](writer, RecordHeader.Error.value) {
  override protected def serialize0(error: DataWriterMessage.LoadEvent.Error): Unit = {
    import error._
    writer.writeString(message)
    writer.writeInt((timestamp - start).toInt)
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

object LogFileDataWriter {
  private[gatling] val LogFileName = "simulation.log"

  def logFile(resultsDirectory: Path, runId: String, create: Boolean): Path =
    simulationLogDirectory(runId, create, resultsDirectory).resolve(LogFileName)
}

final class LogFileDataWriter(resultsDirectory: Path, configuration: GatlingConfiguration) extends DataWriter[FileData]("file-data-writer") {
  override def onInit(init: DataWriterMessage.Init): FileData = {
    import init._

    val simulationLog = LogFileDataWriter.logFile(resultsDirectory, runMessage.runId, create = true)
    val channel = new RandomAccessFile(simulationLog.toFile, "rw").getChannel
    val bb = ByteBuffer.allocate(configuration.data.file.bufferSize)
    val writer = new BufferedFileChannelWriter(channel, bb)
    val scenarios = new ju.HashMap[String, Int]
    init.scenarios.map(_.name).zipWithIndex.foreach { case (scenario, index) =>
      scenarios.put(scenario, index)
    }
    new RunMessageSerializer(writer).serialize(runMessage, assertions, scenarios)

    new FileData(
      new UserStartMessageSerializer(writer, init.runMessage.start, scenarios),
      new UserEndMessageSerializer(writer, init.runMessage.start, scenarios),
      new ResponseMessageSerializer(writer, init.runMessage.start),
      new GroupMessageSerializer(writer, init.runMessage.start),
      new ErrorMessageSerializer(writer, init.runMessage.start),
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
