/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.io.FileOutputStream

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles.simulationLogDirectory
import io.gatling.core.result.Group
import io.gatling.core.session.GroupStackEntry
import io.gatling.core.util.StringHelper.eol
import io.gatling.core.util.UnsynchronizedBufferedOutputStream

object FileDataWriter {

	val emptyField = " "

	val sanitizerPattern = """[\n\r\t]""".r

	/**
	 * Converts whitespace characters that would break the simulation log format into spaces.
	 */
	def sanitize(s: String): String = Option(s) match {
		case Some(s) => sanitizerPattern.replaceAllIn(s, " ")
		case _ => ""
	}

	implicit class RunMessageSerializer(val runMessage: RunMessage) extends AnyVal {

		def getBytes = {
			import runMessage._
			val description = if (runDescription.isEmpty) FileDataWriter.emptyField else runDescription
			val string = s"$simulationClassName\t$simulationId\t${RunMessageType.name}\t$timestamp\t$description$eol"
			string.getBytes(configuration.core.charSet)
		}
	}

	implicit class UserMessageSerializer(val userMessage: UserMessage) extends AnyVal {

		def getBytes = {
			import userMessage._
			val string = s"$scenarioName\t$userId\t${UserMessageType.name}\t${event.name}\t$startDate\t$endDate$eol"
			string.getBytes(configuration.core.charSet)
		}
	}

	// fastring macro won't work inside a value class in 2.10
	object RequestMessageSerializer {

		def serialize(requestMessage: RequestMessage) = {
			import requestMessage._

			val nonEmptyMessage = message match {
				case Some(r) => r
				case None => emptyField
			}
			val serializedGroups = GroupMessageSerializer.serializeGroups(groupStack)
			val serializedExtraInfo = extraInfo.map(info => fast"\t${sanitize(info.toString)}").mkFastring

			fast"$scenario\t$userId\t${RequestMessageType.name}\t$serializedGroups\t$name\t$requestStartDate\t$requestEndDate\t$responseStartDate\t$responseEndDate\t$status\t$nonEmptyMessage$serializedExtraInfo$eol"
		}
	}

	implicit class RequestMessageSerializer(val requestMessage: RequestMessage) extends AnyVal {

		def getBytes = {
			val string = RequestMessageSerializer.serialize(requestMessage).toString
			string.getBytes(configuration.core.charSet)
		}
	}

	// fastring macro won't work inside a value class in 2.10
	object GroupMessageSerializer {

		def serializeGroups(groupStack: List[GroupStackEntry]) = groupStack.reverse.map(_.name).mkFastring(",")

		def deserializeGroups(string: String) = Group(string.split(",").toList)

		def serialize(groupMessage: GroupMessage) = {
			import groupMessage._
			val serializedGroups = serializeGroups(groupStack)
			val group = groupStack.head
			fast"$scenarioName\t$userId\t${GroupMessageType.name}\t$serializedGroups\t$entryDate\t$exitDate\t${group.cumulatedResponseTime}\t${group.oks}\t${group.kos}\t$status$eol"
		}
	}

	implicit class GroupMessageSerializer(val groupMessage: GroupMessage) extends AnyVal {

		def getBytes = {
			val string = GroupMessageSerializer.serialize(groupMessage).toString
			string.getBytes(configuration.core.charSet)
		}
	}
}

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation if a tabulation separated values file
 */
class FileDataWriter extends DataWriter {

	import FileDataWriter._

	private var os: UnsynchronizedBufferedOutputStream = _

	override def onInitializeDataWriter(run: RunMessage, scenarios: Seq[ShortScenarioDescription]) {
		val simulationLog = simulationLogDirectory(run.runId) / "simulation.log"
		val fos = new FileOutputStream(simulationLog.toString)
		system.registerOnTermination(fos.close)
		os = new UnsynchronizedBufferedOutputStream(fos, configuration.data.file.bufferSize)
		os.write(run.getBytes)
	}

	override def onUserMessage(userMessage: UserMessage) { os.write(userMessage.getBytes) }

	override def onGroupMessage(group: GroupMessage) { os.write(group.getBytes) }

	override def onRequestMessage(request: RequestMessage) { os.write(request.getBytes) }

	override def onTerminateDataWriter { os.flush }
}
