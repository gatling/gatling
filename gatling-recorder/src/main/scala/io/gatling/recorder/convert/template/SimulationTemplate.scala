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

package io.gatling.recorder.convert.template

import scala.concurrent.duration._

import io.gatling.commons.util.StringHelper.Eol
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.convert._

private[convert] object SimulationTemplate {
  def apply(requestBodies: Map[Int, DumpedBody], responseBodies: Map[Int, DumpedBody], configuration: RecorderConfiguration): SimulationTemplate =
    new SimulationTemplate(
      configuration.core.pkg,
      configuration.core.className,
      new ProtocolTemplate(configuration),
      new RequestTemplate(requestBodies, responseBodies, configuration)
    )

  private[template] def renderNonBaseUrls(values: Seq[UrlVal]): String =
    values
      .sortBy(_.valName)
      .map(value => s"  val ${value.valName} = ${protectWithTripleQuotes(value.url)}")
      .mkString(Eol)

  private[template] def headersBlockName(id: Int) = s"headers_$id"

  private val MaxElementPerChain = 100
}

private[convert] class SimulationTemplate(
    packageName: String,
    simulationClassName: String,
    protocolTemplate: ProtocolTemplate,
    requestTemplate: RequestTemplate
) {

  import SimulationTemplate._

  private def renderHeaders(headers: Map[Int, Seq[(String, String)]]) = {
    def printHeaders(headers: Seq[(String, String)]) = headers match {
      case Seq((name, value)) =>
        s"Map(${protectWithTripleQuotes(name)} -> ${protectWithTripleQuotes(value)})"
      case _ =>
        val mapContent = headers.map { case (name, value) => s"		${protectWithTripleQuotes(name)} -> ${protectWithTripleQuotes(value)}" }.mkString(",\n")
        s"""Map(
           |$mapContent)""".stripMargin
    }

    headers
      .map { case (headersBlockIndex, headersBlock) => s"""  val ${headersBlockName(headersBlockIndex)} = ${printHeaders(headersBlock)}""" }
      .mkString(s"""
                   |
                   |""".stripMargin)
  }

  private def renderScenarioElement(se: HttpTrafficElement, extractedUris: ExtractedUris) = se match {
    case TagElement(text)        => s"// $text"
    case PauseElement(duration)  => s".pause(${if (duration > 1.second) duration.toSeconds.toString else duration.toString.replace(' ', '.')})"
    case request: RequestElement => s".exec(${requestTemplate.render(simulationClassName, request, extractedUris, mainRequest = true)})"
  }

  private def renderScenario(extractedUris: ExtractedUris, elements: Seq[HttpTrafficElement]) = {
    if (elements.size <= MaxElementPerChain) {
      val scenarioElements = elements
        .map(renderScenarioElement(_, extractedUris))
        .mkString(s"""
                     |    """.stripMargin)

      s"""  val scn = scenario("$simulationClassName")
         |		$scenarioElements""".stripMargin

    } else {
      val chains =
        elements
          .grouped(MaxElementPerChain)
          .toList
          .zipWithIndex
          .map { case (chain, i) =>
            var firstNonTagElement = true
            val chainContent = chain
              .map { element =>
                val prefix = element match {
                  case TagElement(_) => ""
                  case _ =>
                    if (firstNonTagElement) {
                      firstNonTagElement = false
                      ""
                    } else {
                      "."
                    }
                }
                s"$prefix${renderScenarioElement(element, extractedUris)}"
              }
            (i, chainContent)
          }

      s"""${chains.map { case (i, content) => s"  val chain_$i = $content" }.mkString(s"$Eol$Eol")}
         |
         |	val scn = scenario("$simulationClassName").exec(${chains.map { case (i, _) => s"chain_$i" }.mkString(", ")})""".stripMargin
    }
  }

  def render(
      protocol: ProtocolDefinition,
      headers: Map[Int, Seq[(String, String)]],
      scenarioElements: Seq[HttpTrafficElement]
  ): String = {

    val extractedUris = ExtractedUris(scenarioElements)

    val nonBaseUrls: Seq[UrlVal] = extractedUris.nonBaseUrls(protocol.baseUrl)

    s"""${if (packageName.nonEmpty) s"package $packageName$Eol" else ""}
       |import scala.concurrent.duration._
       |
       |import io.gatling.core.Predef._
       |import io.gatling.http.Predef._
       |import io.gatling.jdbc.Predef._
       |
       |class $simulationClassName extends Simulation {
       |
       |	val httpProtocol = ${protocolTemplate.render(protocol)}
       |
       |${renderHeaders(headers)}
       |
       |${renderNonBaseUrls(nonBaseUrls)}
       |
       |${renderScenario(extractedUris, scenarioElements)}
       |
       |	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
       |}
       |""".stripMargin
  }
}
