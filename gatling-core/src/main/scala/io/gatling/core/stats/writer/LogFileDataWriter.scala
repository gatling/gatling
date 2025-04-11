/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path

import scala.jdk.CollectionConverters.MapHasAsScala

import io.gatling.commons.stats.OK
import io.gatling.commons.stats.assertion.Assertion
import io.gatling.core.config.GatlingFiles.simulationLogDirectory

import com.typesafe.scalalogging.StrictLogging

private[writer] final class BufferedFileChannelWriter(channel: FileChannel, bb: ByteBuffer) extends AutoCloseable with StrictLogging {

  // we must start at 1 because we use the opposite value for a cache hit
  // but as -0 == 0, it would always result on a cache miss on the read side
  private var stringCacheCurrentIndex = 1
  private val stringCache = new ju.HashMap[String, jl.Integer]

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

  def writeCachedString(string: String): Unit = {
    val cachedIndex = stringCache.get(string)
    if (cachedIndex == null) {
      writeInt(stringCacheCurrentIndex)
      writeString(string)
      stringCache.put(string, stringCacheCurrentIndex)
      stringCacheCurrentIndex += 1
    } else {
      writeInt(-cachedIndex.intValue)
    }
  }

  def writeString(string: String): Unit =
    if (string.isEmpty) {
      bb.putInt(0)
    } else {
      val value = StringInternals.value(string)
      val valueLength = value.length
      val coder = StringInternals.coder(string)
      ensureCapacity(jl.Byte.BYTES + jl.Integer.BYTES + valueLength)
      bb.putInt(value.length)
      val originalPosition = bb.position
      System.arraycopy(value, 0, bb.array, originalPosition, valueLength)
      bb.position(originalPosition + valueLength)
      bb.put(coder)
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
    groupHierarchy.foreach(writer.writeCachedString)
  }

  def serialize(m: T): Unit = {
    writer.writeByte(header)
    serialize0(m)
  }

  protected def serialize0(m: T): Unit
}

final class RunMessageSerializer(writer: BufferedFileChannelWriter)
    extends DataWriterMessageSerializer[(RunMessage, Seq[Assertion], ju.Map[String, Int])](writer, RecordHeader.Run.value) {

  // WARNING do not remove or serialization will happen with a wrong format
  import io.gatling.shared.model.assertion.AssertionPicklers._

  import boopickle.Default._

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
      val byteBuffer = Pickle.intoBytes(assertion)
      writer.writeByteBuffer(byteBuffer)
    }
  }
}

final class UserMessageSerializer(writer: BufferedFileChannelWriter, runStart: Long, scenarios: ju.Map[String, Int])
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.User](writer, RecordHeader.User.value) {
  override protected def serialize0(user: DataWriterMessage.LoadEvent.User): Unit = {
    import user._
    writer.writeInt(scenarios.get(scenario))
    writer.writeBoolean(start)
    writer.writeInt((timestamp - runStart).toInt)
  }
}

final class ResponseMessageSerializer(writer: BufferedFileChannelWriter, runStart: Long)
    extends DataWriterMessageSerializer[DataWriterMessage.LoadEvent.Response](writer, RecordHeader.Request.value) {

  override protected def serialize0(response: DataWriterMessage.LoadEvent.Response): Unit = {
    import response._
    writeGroups(groupHierarchy)
    writer.writeCachedString(name)
    writer.writeInt((startTimestamp - runStart).toInt)
    writer.writeInt((endTimestamp - runStart).toInt)
    writer.writeBoolean(status == OK)
    writer.writeCachedString(message.getOrElse(""))
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
    writer.writeCachedString(message)
    writer.writeInt((timestamp - start).toInt)
  }
}

final class FileData(
    val userMessageSerializer: UserMessageSerializer,
    val responseMessageSerializer: ResponseMessageSerializer,
    val groupMessageSerializer: GroupMessageSerializer,
    val errorMessageSerializer: ErrorMessageSerializer,
    val writer: BufferedFileChannelWriter
) extends DataWriterData

private[gatling] object LogFileDataWriter {
  private[gatling] val LogFileName = "simulation.log"
  private val BufferSize = 8192

  def logFile(resultsDirectory: Path, runId: String, create: Boolean): Path =
    simulationLogDirectory(runId, create, resultsDirectory).resolve(LogFileName)

  def apply(
      runMessage: RunMessage,
      scenarios: Seq[ShortScenarioDescription],
      assertions: Seq[Assertion],
      resultsDirectory: Path
  ): LogFileDataWriter = {
    StringInternals.checkAvailability()
    val simulationLog = LogFileDataWriter.logFile(resultsDirectory, runMessage.runId, create = true)
    val channel = new RandomAccessFile(simulationLog.toFile, "rw").getChannel
    val bb = ByteBuffer.allocate(BufferSize)
    val writer = new BufferedFileChannelWriter(channel, bb)
    val scenariosMap = new ju.HashMap[String, Int]
    scenarios.map(_.name).zipWithIndex.foreach { case (scenario, index) =>
      scenariosMap.put(scenario, index)
    }

    new LogFileDataWriter(
      runMessage,
      scenariosMap,
      assertions,
      writer
    )
  }
}

private[gatling] final class LogFileDataWriter private (
    runMessage: RunMessage,
    scenariosMap: ju.HashMap[String, Int],
    assertions: Seq[Assertion],
    writer: BufferedFileChannelWriter
) extends DataWriter[FileData]("file-data-writer") {

  override def onInit(): FileData = {
    new RunMessageSerializer(writer).serialize(runMessage, assertions, scenariosMap)

    new FileData(
      new UserMessageSerializer(writer, runMessage.start, scenariosMap),
      new ResponseMessageSerializer(writer, runMessage.start),
      new GroupMessageSerializer(writer, runMessage.start),
      new ErrorMessageSerializer(writer, runMessage.start),
      writer
    )
  }

  override def onFlush(data: FileData): Unit = {}

  override def onMessage(message: DataWriterMessage.LoadEvent, data: FileData): Unit =
    message match {
      case user: DataWriterMessage.LoadEvent.User         => data.userMessageSerializer.serialize(user)
      case group: DataWriterMessage.LoadEvent.Group       => data.groupMessageSerializer.serialize(group)
      case response: DataWriterMessage.LoadEvent.Response => data.responseMessageSerializer.serialize(response)
      case error: DataWriterMessage.LoadEvent.Error       => data.errorMessageSerializer.serialize(error)
    }

  override def onCrash(cause: String, data: FileData): Unit = {}

  override def onStop(data: FileData): Unit =
    data.writer.close()
}
