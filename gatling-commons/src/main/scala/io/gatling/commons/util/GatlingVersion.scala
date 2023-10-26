/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.commons.util

import java.net.{ HttpURLConnection, URI }
import java.nio.charset.StandardCharsets
import java.time.{ Instant, ZoneOffset, ZonedDateTime }
import java.time.format.DateTimeFormatter
import java.util.ResourceBundle
import java.util.prefs.Preferences

import scala.util.{ Success, Try }
import scala.util.control.NonFatal

import com.typesafe.scalalogging.StrictLogging

object GatlingVersion {
  val ThisVersion: GatlingVersion = {
    val bundle = ResourceBundle.getBundle("gatling-version")
    GatlingVersion(
      bundle.getString("version"),
      ZonedDateTime.parse(bundle.getString("release-date"), DateTimeFormatter.ISO_DATE_TIME)
    )
  }

  lazy val LatestRelease: Option[GatlingVersion] = LatestGatlingRelease.load()
}

final case class GatlingVersion(fullVersion: String, releaseDate: ZonedDateTime) {
  val minorVersion: String = fullVersion.split('.').take(2).mkString(".")
  val majorVersion: String = fullVersion.substring(0, fullVersion.indexOf('.'))
  def isDev: Boolean = fullVersion.endsWith("-SNAPSHOT")
  def isEnterprise: Boolean = fullVersion.contains(".FL")
}

private object LatestGatlingRelease extends StrictLogging {
  private sealed abstract class FetchResult(val lastCheckTimestamp: Long, maxAgeMillis: Long) extends Product with Serializable {
    def valid(nowMillis: Long): Boolean = nowMillis - lastCheckTimestamp < maxAgeMillis
  }

  private object FetchResult {
    final case class Success(override val lastCheckTimestamp: Long, latestRelease: GatlingVersion) extends FetchResult(lastCheckTimestamp, 24 * 3600)
    final case class Failure(override val lastCheckTimestamp: Long) extends FetchResult(lastCheckTimestamp, 3600)
  }

  private val Prefs = Preferences.userRoot().node("/io/gatling/latestVersion")
  private val LastCheckTimestampPref = "lastCheckTimestamp"
  private val LastCheckSuccessPref = "lastCheckSuccess"
  private val LatestReleaseNumberPref = "latestReleaseNumber"
  private val LatestReleaseDatePref = "latestReleaseDate"

  private val MavenCentralQuery = "https://search.maven.org/solrsearch/select?q=g:io.gatling+AND+a:gatling-core+AND+p:jar&rows=1&wt=json&core=gav"
  private val MavenCentralQueryTimeoutMillis = 1000
  private val MavenCentralQueryVersionRegex = """"v":\s*"(.+?)"""".r
  private val MavenCentralQueryTimestampRegex = """"timestamp":\s*(\d+)""".r

  private def loadPersisted(): Try[FetchResult] =
    Try {
      val lastCheckTimestamp = Prefs.getLong(LastCheckTimestampPref, 0)
      if (Prefs.getBoolean(LastCheckSuccessPref, false)) {
        FetchResult.Success(
          lastCheckTimestamp,
          GatlingVersion(
            Option(Prefs.get(LatestReleaseNumberPref, null)).getOrElse(throw new IllegalStateException(s"$LatestReleaseNumberPref is missing")),
            Option(Prefs.get(LatestReleaseDatePref, null))
              .map(ZonedDateTime.parse(_, DateTimeFormatter.ISO_DATE_TIME))
              .getOrElse(throw new IllegalStateException(s"$LatestReleaseDatePref is missing"))
          )
        )
      } else {
        FetchResult.Failure(lastCheckTimestamp)
      }
    }

  private def persist(lastCheck: FetchResult): Try[Unit] =
    Try {
      Prefs.putLong(LastCheckTimestampPref, lastCheck.lastCheckTimestamp)
      lastCheck match {
        case FetchResult.Success(_, latestRelease) =>
          Prefs.putBoolean(LastCheckSuccessPref, true)
          Prefs.put(LatestReleaseNumberPref, latestRelease.fullVersion)
          Prefs.putLong(LastCheckSuccessPref, latestRelease.releaseDate.toInstant.toEpochMilli)

        case _ =>
          Prefs.putBoolean(LastCheckSuccessPref, false)
          Prefs.remove(LatestReleaseNumberPref)
          Prefs.remove(LastCheckSuccessPref)
      }
      Prefs.flush()
    }

  private def fetchLatestReleaseFromMavenCentral(): Try[GatlingVersion] =
    Try {
      val conn = new URI(MavenCentralQuery).toURL.openConnection().asInstanceOf[HttpURLConnection]

      try {
        conn.setReadTimeout(MavenCentralQueryTimeoutMillis)
        conn.setConnectTimeout(MavenCentralQueryTimeoutMillis)
        conn.setDoInput(true)
        conn.setDoOutput(false)
        conn.setUseCaches(true)
        conn.setRequestMethod("GET")
        conn.setRequestProperty("Connection", "close")

        val response = new String(conn.getInputStream.readAllBytes(), StandardCharsets.UTF_8)
        parseMavenCentralResponse(response)
      } finally {
        conn.disconnect()
      }
    }

  private[util] def parseMavenCentralResponse(response: String): GatlingVersion = {
    val version =
      MavenCentralQueryVersionRegex
        .findFirstMatchIn(response)
        .getOrElse(throw new IllegalArgumentException(s"Failed to find version field in $response"))
        .group(1)
    val timestamp =
      MavenCentralQueryTimestampRegex
        .findFirstMatchIn(response)
        .getOrElse(throw new IllegalArgumentException(s"Failed to find timestamp field in $response"))
        .group(1)
        .toLong
    GatlingVersion(version, ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC))
  }

  private implicit final class OnFailureTry[T](val t: Try[T]) extends AnyVal {
    def logDebugOnFailure(message: String): Try[T] =
      t.recoverWith { case NonFatal(e) =>
        logger.debug(message, e)
        t
      }
  }

  def load(): Option[GatlingVersion] = {
    val now = System.currentTimeMillis()
    loadPersisted().logDebugOnFailure("Failed to load persisted latest release") match {
      case Success(lastCheck) if lastCheck.valid(now) =>
        lastCheck match {
          case FetchResult.Success(_, latestRelease) => Some(latestRelease)
          case _                                     => None
        }

      case _ =>
        val maybeFetched = fetchLatestReleaseFromMavenCentral().logDebugOnFailure("Failed to fetch latest release from maven central")
        val fetchResult = maybeFetched.fold(
          _ => FetchResult.Failure(now),
          FetchResult.Success(now, _)
        )
        persist(fetchResult).logDebugOnFailure("Failed to persist last version check")
        maybeFetched.toOption
    }
  }
}
