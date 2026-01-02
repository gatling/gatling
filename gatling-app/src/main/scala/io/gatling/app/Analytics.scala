/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.app

import java.net.{ HttpURLConnection, URI }
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

import scala.util.{ Try, Using }
import scala.util.control.NonFatal

import io.gatling.commons.util.{ GatlingVersion, Java }
import io.gatling.core.json.Json

import io.netty.util.internal.PlatformDependent

object Analytics {
  private val ApiKeyDev = "27a3799b1445c6ab08674c6b8fa3b956"
  private val ApiKeyProd = "4ba61bcc5dc0854ac5ee8cafa62e403b"

  val UsedGatlingModules: Set[String] = {
    val findLoadedClassMethod = classOf[ClassLoader].getDeclaredMethod("findLoadedClass", classOf[String])
    findLoadedClassMethod.setAccessible(true)
    def isClassLoaded(className: String): Boolean = findLoadedClassMethod.invoke(getClass.getClassLoader, className) != null

    Set(
      Option.when(isClassLoaded("io.gatling.http.action.HttpRequestAction"))("http"),
      Option.when(isClassLoaded("io.gatling.http.action.ws.WsAction"))("ws"),
      Option.when(isClassLoaded("io.gatling.http.action.sse.SseAction"))("sse"),
      Option.when(isClassLoaded("io.gatling.jms.protocol.JmsProtocol"))("jms"),
      Option.when(isClassLoaded("io.gatling.mqtt.protocol.MqttProtocol"))("mqtt"),
      Option.when(isClassLoaded("io.gatling.grpc.protocol.GrpcProtocol"))("grpc"),
      Option.when(isClassLoaded("io.gatling.postman.enterprise.Usage"))("postman")
    ).flatten
  }

  def send(simulationClass: SimulationClass, explicitLauncher: Option[String], buildToolVersion: Option[String]): Unit = {
    val apiKey = if (GatlingVersion.ThisVersion.isDev) ApiKeyDev else ApiKeyProd
    val programmingLanguage = simulationClass match {
      case SimulationClass.Java(_) =>
        Try {
          getClass.getClassLoader.loadClass("kotlin.KotlinVersion")
          "kotlin"
        }.toOption.getOrElse("java")
      case SimulationClass.Scala(_)         => "scala"
      case SimulationClass.JavaScript(_, _) => "javascript"
    }
    val launcher = explicitLauncher
      .orElse(
        if (sys.props.get("java.class.path").exists(_.contains("idea"))) {
          Some("idea")
        } else if (
          // eclipse on MacOS
          sys.env.get("__CFBundleIdentifier").exists(_.contains("eclipse")) ||
          // eclipse on Windows with bundle JRE
          sys.props.get("java.library.path").exists(_.contains("eclipse"))
        ) {
          Some("eclipse")
        } else {
          None
        }
      )

    val userPropertiesBase = Map(
      "java_version_major" -> Java.MajorVersion.toString,
      "java_version_full" -> Java.FullVersion,
      "gatling_version_major" -> GatlingVersion.ThisVersion.majorVersion,
      "gatling_version_minor" -> GatlingVersion.ThisVersion.minorVersion,
      "gatling_version_full" -> GatlingVersion.ThisVersion.fullVersion,
      "gatling_version_enterprise" -> GatlingVersion.ThisVersion.isEnterprise,
      "gatling_modules" -> Json.stringify(UsedGatlingModules, isRootObject = false),
      "programming_language" -> programmingLanguage,
      "system_os" -> PlatformDependent.normalizedOs,
      "system_arch" -> PlatformDependent.normalizedArch
    )

    val launcherProperties = launcher.fold(Map.empty[String, String])(l => Map("launcher" -> l))
    val buildToolVersionProperties = buildToolVersion.fold(Map.empty[String, String])(btv => Map("build-tool-version" -> btv))

    val jsonUserProperties = Json.stringify(userPropertiesBase ++ launcherProperties ++ buildToolVersionProperties, isRootObject = false)

    val bodyBytes =
      s"""{
         |   "api_key":"$apiKey",
         |   "events":[
         |      {
         |         "device_id":"${UUID.randomUUID()}",
         |         "event_type":"gatling_run",
         |         "ip":"$$remote",
         |         "user_properties": $jsonUserProperties
         |      }
         |   ]
         |}""".stripMargin
        .getBytes(UTF_8)

    val url = URI.create("https://api.eu.amplitude.com/2/httpapi").toURL

    val thread = new Thread(() =>
      try {
        val conn = url.openConnection().asInstanceOf[HttpURLConnection]

        try {
          conn.setReadTimeout(2000)
          conn.setConnectTimeout(2000)
          conn.setDoInput(true)
          conn.setDoOutput(true)
          conn.setUseCaches(false)
          conn.setRequestMethod("POST")
          conn.setRequestProperty("Connection", "close")
          conn.setRequestProperty("Content-Length", bodyBytes.length.toString)

          Using.resource(conn.getOutputStream) { os =>
            os.write(bodyBytes)
            os.flush()

            // wait for the response (at least one byte) before closing
            conn.getInputStream.read()
          }
        } finally {
          conn.disconnect()
        }
      } catch {
        case NonFatal(_) =>
      }
    )
    thread.setDaemon(true)
    thread.start()
  }
}
