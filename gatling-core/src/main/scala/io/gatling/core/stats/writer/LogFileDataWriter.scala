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
package io.gatling.core.stats.writer

import java.io.RandomAccessFile
import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.charset.{ CharsetEncoder, CoderResult }
import java.nio.channels.FileChannel

import scala.util.control.NonFatal

import io.gatling.commons.stats.assertion.Assertion
import io.gatling.commons.util.StringHelper._
import io.gatling.commons.util.PathHelper._
import io.gatling.core.config.GatlingFiles.simulationLogDirectory

import boopickle.Default._
import com.typesafe.scalalogging.StrictLogging
import com.dongxiguo.fastring.Fastring.Implicits._
import jodd.util.Base64

object LogFileDataWriter extends StrictLogging {

  val Separator = '\t'

  object SanitizableString {
    val SanitizerPattern = """[\n\r\t]""".r
  }

  implicit class SanitizableString(val string: String) extends AnyVal {

    /**
     * Converts whitespace characters that would break the simulation log format into spaces.
     */
    def sanitize = SanitizableString.SanitizerPattern.replaceAllIn(string, " ")
  }

  sealed trait DataWriterMessageSerializer[T] {

    def serializeGroups(groupHierarchy: List[String]): Fastring = groupHierarchy.mkFastring(",")

    def serialize(m: T): Fastring
  }

  implicit val RunMessageSerializer = new DataWriterMessageSerializer[RunMessage] {

    def serialize(runMessage: RunMessage): Fastring = {
      import runMessage._
      val description = if (runDescription.isEmpty) " " else runDescription
      fast"${RunRecordHeader.value}$Separator$simulationClassName$Separator${userDefinedSimulationId.getOrElse("")}$Separator$defaultSimulationId$Separator$start$Separator$description${Separator}2.0$Eol"
    }
  }

  implicit val UserMessageSerializer = new DataWriterMessageSerializer[UserMessage] {

    def serialize(user: UserMessage): Fastring = {
      import user._
      fast"${UserRecordHeader.value}$Separator${session.scenario}$Separator${session.userId}$Separator${event.name}$Separator${session.startDate}$Separator$timestamp$Eol"
    }
  }

  implicit val ResponseMessageSerializer = new DataWriterMessageSerializer[ResponseMessage] {

    private def serializeExtraInfo(extraInfo: List[Any]): Fastring =
      try {
        extraInfo.map(info => fast"$Separator${info.toString.sanitize}").mkFastring
      } catch {
        case NonFatal(e) =>
          logger.error("Crash on extraInfo serialization", e)
          EmptyFastring
      }

    private def serializeMessage(message: Option[String]): String =
      message match {
        case Some(m) => m.sanitize
        case None    => " "
      }

    def serialize(response: ResponseMessage): Fastring = {
      import response._
      import timings._
      fast"${RequestRecordHeader.value}$Separator$scenario$Separator$userId$Separator${serializeGroups(groupHierarchy)}$Separator$name$Separator$startTimestamp$Separator$endTimestamp$Separator$status$Separator${serializeMessage(message)}${serializeExtraInfo(extraInfo)}$Eol"
    }
  }

  implicit val GroupMessageSerializer = new DataWriterMessageSerializer[GroupMessage] {

    def serialize(group: GroupMessage): Fastring = {
      import group._
      fast"${GroupRecordHeader.value}$Separator$scenario$Separator$userId$Separator${serializeGroups(groupHierarchy)}$Separator$startTimestamp$Separator$endTimestamp$Separator$cumulatedResponseTime$Separator$status$Eol"
    }
  }

  implicit val AssertionSerializer = new DataWriterMessageSerializer[Assertion] {

    def serialize(assertion: Assertion): Fastring = {

      val byteBuffer = Pickle.intoBytes(assertion)
      val bytes = new Array[Byte](byteBuffer.remaining)
      byteBuffer.get(bytes)
      val base64String = Base64.encodeToString(bytes)

      fast"${AssertionRecordHeader.value}$Separator$base64String$Eol"
    }
  }

  implicit val ErrorMessageSerializer = new DataWriterMessageSerializer[ErrorMessage] {

    def serialize(error: ErrorMessage): Fastring = {
      import error._
      fast"${GroupRecordHeader.value}$Separator$message$Separator$date$Eol"
    }
  }
}

case class FileData(limit: Int, buffer: ByteBuffer, encoder: CharsetEncoder, channel: FileChannel) extends DataWriterData

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation if a tabulation separated values file
 */
class LogFileDataWriter extends DataWriter[FileData] {

  import LogFileDataWriter._

  def onInit(init: Init): FileData = {

    import init._

    val simulationLog = simulationLogDirectory(runMessage.runId)(configuration) / "simulation.log"

    val limit = configuration.data.file.bufferSize

    val channel = new RandomAccessFile(simulationLog.toFile, "rw").getChannel

    val data = FileData(limit, ByteBuffer.allocate(limit * 2), configuration.core.charset.newEncoder, channel)

    system.registerOnTermination(channel.close())
    assertions.foreach(assertion => push(assertion, data))
    push(runMessage, data)

    data
  }

  override def onFlush(data: FileData): Unit = {}

  private def flush(data: FileData, overflown: Boolean): Unit = {
    import data._
    buffer.flip()
    while (buffer.hasRemaining) {
      channel.write(buffer)
    }
    if (overflown) {
      logger.error("Buffer overflow, you shouldn't be logging that much data. Truncating.")
      channel.write(ByteBuffer.wrap(EolBytes))
    }
    buffer.clear()
  }

  private def push[T](message: T, data: FileData)(implicit serializer: DataWriterMessageSerializer[T]): Unit = {

    import data._

    val fs = serializer.serialize(message)
    var overflow = false

    for (string <- fs) {
      val coderResult = encoder.encode(CharBuffer.wrap(string.unsafeChars), buffer, false)
      overflow = coderResult.isOverflow
    }
    if (buffer.position >= limit || overflow)
      flush(data, overflown = overflow)
  }

  override def onMessage(message: LoadEventMessage, data: FileData): Unit = message match {
    case user: UserMessage         => push(user, data)
    case group: GroupMessage       => push(group, data)
    case response: ResponseMessage => push(response, data)
    case error: ErrorMessage       => push(error, data)
    case _                         =>
  }

  override def onCrash(cause: String, data: FileData): Unit = {}

  override def onStop(data: FileData): Unit = {
    flush(data, overflown = false)
    data.channel.force(true)
  }
}
