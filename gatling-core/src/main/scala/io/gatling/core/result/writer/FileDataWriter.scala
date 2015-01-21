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
package io.gatling.core.result.writer

import java.io.RandomAccessFile
import java.nio.{ CharBuffer, ByteBuffer }
import java.nio.channels.FileChannel

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.core.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles.simulationLogDirectory
import io.gatling.core.util.StringHelper._
import io.gatling.core.util.PathHelper._

object FileDataWriter {

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
      fast"$simulationClassName$Separator$simulationId$Separator${RunRecordHeader.value}$Separator$start$Separator$description${Separator}2.0$Eol"
    }
  }

  implicit val UserMessageSerializer = new DataWriterMessageSerializer[UserMessage] {

    def serialize(userMessage: UserMessage): Fastring = {
      import userMessage._
      fast"$scenario$Separator$userId$Separator${UserRecordHeader.value}$Separator${event.name}$Separator$startDate$Separator$endDate$Eol"
    }
  }

  implicit val RequestEndMessageSerializer = new DataWriterMessageSerializer[RequestEndMessage] {

    private def serializeExtraInfo(extraInfo: List[Any]): Fastring =
      extraInfo.map {
        case info: CharSequence => fast"$Separator${info.toString.sanitize}"
        case info               => fast"$info"
      }.mkFastring

    private def serializeMessage(message: Option[String]): String =
      message match {
        case Some(m) => m.sanitize
        case None    => " "
      }

    def serialize(requestMessage: RequestEndMessage): Fastring = {
      import requestMessage._
      import timings._
      fast"$scenario$Separator$userId$Separator${RequestRecordHeader.value}$Separator${serializeGroups(groupHierarchy)}$Separator$name$Separator$requestStartDate$Separator$requestEndDate$Separator$responseStartDate$Separator$responseEndDate$Separator$status$Separator${serializeMessage(message)}${serializeExtraInfo(extraInfo)}$Eol"
    }
  }

  implicit val GroupMessageSerializer = new DataWriterMessageSerializer[GroupMessage] {

    def serialize(groupMessage: GroupMessage): Fastring = {
      import groupMessage._
      fast"$scenario$Separator$userId$Separator${GroupRecordHeader.value}$Separator${serializeGroups(groupHierarchy)}$Separator$startDate$Separator$endDate$Separator${group.cumulatedResponseTime}$Separator${group.oks}$Separator${group.kos}$Separator$status$Eol"
    }
  }

  implicit val AssertionSerializer = new DataWriterMessageSerializer[Assertion] {

    def serialize(assertion: Assertion): Fastring = fast"${assertion.serialized}$Eol"
  }
}

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation if a tabulation separated values file
 */
class FileDataWriter extends DataWriter {

  import FileDataWriter._

  private val limit = configuration.data.file.bufferSize
  private val buffer: ByteBuffer = ByteBuffer.allocateDirect(limit * 2)
  private val encoder = configuration.core.charset.newEncoder
  private var channel: FileChannel = _

  private def flush(): Unit = {
    buffer.flip()
    while (buffer.hasRemaining)
      channel.write(buffer)
    buffer.clear()
  }

  private def push[T](message: T)(implicit serializer: DataWriterMessageSerializer[T]): Unit = {

    val fs = serializer.serialize(message)

    for (string <- fs)
      encoder.encode(CharBuffer.wrap(string.unsafeChars), buffer, false)
    if (buffer.position >= limit)
      flush()
  }

  override def onInitializeDataWriter(assertions: Seq[Assertion], run: RunMessage, scenarios: Seq[ShortScenarioDescription]): Unit = {
    val simulationLog = simulationLogDirectory(run.runId) / "simulation.log"
    channel = new RandomAccessFile(simulationLog.toFile, "rw").getChannel
    buffer.clear()
    system.registerOnTermination(channel.close())
    assertions.foreach(assertion => push(assertion))
    push(run)
  }

  override def onFlush(timestamp: Long): Unit = {}

  override def onMessage(message: LoadEventMessage): Unit = message match {
    case user: UserMessage          => push(user)
    case group: GroupMessage        => push(group)
    case request: RequestEndMessage => push(request)
    case _                          =>
  }

  override def onTerminateDataWriter(): Unit = {
    flush()
    channel.force(true)
  }
}
