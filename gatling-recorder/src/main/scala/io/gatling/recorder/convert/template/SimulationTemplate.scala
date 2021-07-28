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
      configuration.core.format,
      new ProtocolTemplate(configuration),
      new RequestTemplate(requestBodies, responseBodies, configuration)
    )

  private[template] def renderNonBaseUrls(values: Seq[UrlVal], format: Format): String = {
    val referenceType = format match {
      case Format.Scala | Format.Kotlin  => "private val"
      case Format.Java11 | Format.Java17 => "private String"
      case Format.Java8                  => "String"
    }

    if (values.isEmpty) {
      ""
    } else {
      values
        .sortBy(_.valName)
        .map(value => s"$referenceType ${value.valName} = ${value.url.protect(format)}${format.lineTermination}")
        .mkString(Eol, s"$Eol$Eol", "")
    }
  }

  private[template] def headersBlockName(id: Int) = s"headers_$id"

  private val MaxElementPerChain = 100
}

private[convert] class SimulationTemplate(
    packageName: String,
    simulationClassName: String,
    format: Format,
    protocolTemplate: ProtocolTemplate,
    requestTemplate: RequestTemplate
) {

  import SimulationTemplate._

  private def renderHeaders(headers: Map[Int, Seq[(String, String)]]) =
    headers
      .map { case (headersBlockIndex, headersBlock) =>
        val headerReference = headersBlockName(headersBlockIndex)
        val protectedHeaders = headersBlock.map { case (name, value) => (name.protect(format), value.protect(format)) }

        format match {
          case Format.Scala =>
            protectedHeaders match {
              case Seq((protectedName, protectedValue)) =>
                s"private val $headerReference = Map($protectedName -> $protectedValue)"
              case _ =>
                s"""private val $headerReference = Map(
                   |${protectedHeaders.map { case (protectedName, protectedValue) => s"		$protectedName -> $protectedValue" }.mkString(s",$Eol")}
                   |)""".stripMargin
            }
          case Format.Kotlin =>
            protectedHeaders match {
              case Seq((protectedName, protectedValue)) =>
                s"private val $headerReference = mapOf($protectedName to $protectedValue)"
              case _ =>
                s"""private val $headerReference = mapOf(
                   |${protectedHeaders.map { case (protectedName, protectedValue) => s"  $protectedName to $protectedValue" }.mkString(s",$Eol")}
                   |)""".stripMargin
            }

          case Format.Java11 | Format.Java17 =>
            protectedHeaders match {
              case Seq((protectedName, protectedValue)) =>
                s"private Map<CharSequence, String> $headerReference = Map.of($protectedName, $protectedValue);"
              case _ =>
                s"""private Map<CharSequence, String> $headerReference = Map.ofEntries(
                   |${protectedHeaders.map { case (protectedName, protectedValue) => s"  Map.entry($protectedName, $protectedValue)" }.mkString(s",$Eol")}
                   |);""".stripMargin
            }

          case _ =>
            s"""Map<CharSequence, String> $headerReference = new HashMap<>();
               |${protectedHeaders
              .map { case (protectedName, protectedValue) => s"$headerReference.put($protectedName, $protectedValue);" }
              .mkString(Eol)}""".stripMargin
        }
      }
      .mkString(Eol, s"$Eol$Eol", "")

  private def renderScenarioElement(se: HttpTrafficElement, extractedUris: ExtractedUris) = se match {
    case TagElement(text) => s"// $text"
    case PauseElement(duration) =>
      val pauseString =
        if (duration > 1.second) {
          duration.toSeconds.toString
        } else {
          format match {
            case Format.Scala => s"${duration.toMillis}.milliseconds"
            case _            => s"Duration.ofMillis(${duration.toMillis})"
          }
        }

      s".pause($pauseString)"
    case request: RequestElement =>
      s""".exec(
         |${requestTemplate.render(simulationClassName, request, extractedUris).indent(2)}
         |)""".stripMargin
  }

  private def renderScenario(extractedUris: ExtractedUris, elements: Seq[HttpTrafficElement]) = {
    val scenarioReferenceType = format match {
      case Format.Scala | Format.Kotlin  => "private val"
      case Format.Java11 | Format.Java17 => "private ScenarioBuilder"
      case Format.Java8                  => "ScenarioBuilder"
    }

    if (elements.size <= MaxElementPerChain) {
      val scenarioElements = elements
        .map(renderScenarioElement(_, extractedUris))
        .mkString(Eol)

      s"""$scenarioReferenceType scn = scenario("$simulationClassName")
         |${scenarioElements.indent(2)}${format.lineTermination}""".stripMargin

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

      val chainReferenceType = format match {
        case Format.Scala | Format.Kotlin  => "private val"
        case Format.Java11 | Format.Java17 => "private ChainBuilder"
        case Format.Java8                  => "ChainBuilder"
      }

      s"""${chains.map { case (i, content) => s"$chainReferenceType chain_$i = $content" }.mkString(s"${format.lineTermination}$Eol$Eol")}
         |
         |$scenarioReferenceType scn = scenario("$simulationClassName")
         |  .exec(${chains.map { case (i, _) => s"chain_$i" }.mkString(", ")})${format.lineTermination}""".stripMargin
    }
  }

  def render(
      protocol: ProtocolDefinition,
      headers: Map[Int, Seq[(String, String)]],
      scenarioElements: Seq[HttpTrafficElement]
  ): String = {
    val extractedUris = ExtractedUris(scenarioElements, format)
    val nonBaseUrls: Seq[UrlVal] = extractedUris.nonBaseUrls(protocol.baseUrl)

    format match {
      case Format.Scala =>
        s"""${if (packageName.nonEmpty) s"package $packageName$Eol" else ""}
           |import scala.concurrent.duration._
           |
           |import io.gatling.core.Predef._
           |import io.gatling.http.Predef._
           |import io.gatling.jdbc.Predef._
           |
           |class $simulationClassName extends Simulation {
           |
           |${protocolTemplate.render(protocol).indent(2)}
           |${renderHeaders(headers).indent(2)}
           |${renderNonBaseUrls(nonBaseUrls, format).indent(2)}
           |
           |${renderScenario(extractedUris, scenarioElements).indent(2)}
           |
           |	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
           |}
           |""".stripMargin

      case Format.Kotlin =>
        s"""${if (packageName.nonEmpty) s"package $packageName$Eol" else ""}
           |import java.time.Duration
           |
           |import io.gatling.core.javaapi.*
           |import io.gatling.http.javaapi.*
           |import io.gatling.jdbc.javaapi.*
           |
           |import io.gatling.core.javaapi.Predef.*
           |import io.gatling.http.javaapi.Predef.*
           |import io.gatling.jdbc.javaapi.Predef.*
           |
           |class $simulationClassName : Simulation() {
           |
           |${protocolTemplate.render(protocol).indent(2)}
           |${renderHeaders(headers).indent(2)}
           |${renderNonBaseUrls(nonBaseUrls, format).indent(2)}
           |
           |${renderScenario(extractedUris, scenarioElements).indent(2)}
           |
           |  init {
           |	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol)
           |  }
           |}
           |""".stripMargin

      case Format.Java11 | Format.Java17 =>
        s"""${if (packageName.nonEmpty) s"package $packageName;$Eol" else ""}
           |import java.time.*;
           |import java.util.*;
           |
           |import io.gatling.core.javaapi.*;
           |import io.gatling.http.javaapi.*;
           |import io.gatling.jdbc.javaapi.*;
           |
           |import static io.gatling.core.javaapi.Predef.*;
           |import static io.gatling.http.javaapi.Predef.*;
           |import static io.gatling.jdbc.javaapi.Predef.*;
           |
           |public class $simulationClassName extends Simulation {
           |
           |${protocolTemplate.render(protocol).indent(2)}
           |${renderHeaders(headers).indent(2)}
           |${renderNonBaseUrls(nonBaseUrls, format).indent(2)}
           |
           |${renderScenario(extractedUris, scenarioElements).indent(2)}
           |
           |  public $simulationClassName() {
           |	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
           |  }
           |}
           |""".stripMargin

      case Format.Java8 =>
        s"""${if (packageName.nonEmpty) s"package $packageName;$Eol" else ""}
           |import java.time.*;
           |import java.util.*;
           |
           |import io.gatling.core.javaapi.*;
           |import io.gatling.http.javaapi.*;
           |import io.gatling.jdbc.javaapi.*;
           |
           |import static io.gatling.core.javaapi.Predef.*;
           |import static io.gatling.http.javaapi.Predef.*;
           |import static io.gatling.jdbc.javaapi.Predef.*;
           |
           |public class $simulationClassName extends Simulation {
           |
           |  public $simulationClassName() {
           |${protocolTemplate.render(protocol).indent(4)}
           |${renderHeaders(headers).indent(4)}
           |${renderNonBaseUrls(nonBaseUrls, format).indent(4)}
           |
           |${renderScenario(extractedUris, scenarioElements).indent(4)}
           |
           |	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
           |  }
           |}
           |""".stripMargin
    }
  }
}
