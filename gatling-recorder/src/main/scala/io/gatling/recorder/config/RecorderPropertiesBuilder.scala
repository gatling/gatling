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
package io.gatling.recorder.config

import java.util.{ List => JList }

import scala.collection.mutable

import io.gatling.recorder.config.ConfigKeys._

class RecorderPropertiesBuilder {

  private val props = mutable.Map.empty[String, Any]

  def mode(mode: RecorderMode): Unit =
    props += core.Mode -> mode.toString

  def encoding(encoding: String): Unit =
    props += core.Encoding -> encoding

  def simulationOutputFolder(folder: String): Unit =
    props += core.SimulationOutputFolder -> folder

  def bodiesFolder(folder: String): Unit =
    props += core.BodiesFolder -> folder

  def simulationPackage(pkg: String): Unit =
    props += core.Package -> pkg

  def simulationClassName(className: String): Unit =
    props += core.ClassName -> className

  def thresholdForPauseCreation(threshold: String): Unit =
    props += core.ThresholdForPauseCreation -> threshold

  def saveConfig(status: Boolean): Unit =
    props += core.SaveConfig -> status

  def headless(status: Boolean): Unit =
    props += core.Headless -> status

  def harFilePath(path: String): Unit =
    props += core.HarFilePath -> path

  def filterStrategy(strategy: String): Unit =
    props += filters.FilterStrategy -> strategy

  def whitelist(patterns: JList[String]): Unit =
    props += filters.WhitelistPatterns -> patterns

  def blacklist(patterns: JList[String]): Unit =
    props += filters.BlacklistPatterns -> patterns

  def automaticReferer(status: Boolean): Unit =
    props += http.AutomaticReferer -> status

  def followRedirect(status: Boolean): Unit =
    props += http.FollowRedirect -> status

  def inferHtmlResources(status: Boolean): Unit =
    props += http.InferHtmlResources -> status

  def removeCacheHeaders(status: Boolean): Unit =
    props += http.RemoveCacheHeaders -> status

  def checkResponseBodies(status: Boolean): Unit =
    props += http.CheckResponseBodies -> status

  def localPort(port: Int): Unit =
    props += proxy.Port -> port

  def proxyHost(host: String): Unit =
    props += proxy.outgoing.Host -> host

  def proxyUsername(username: String): Unit =
    props += proxy.outgoing.Username -> username

  def proxyPassword(password: String): Unit =
    props += proxy.outgoing.Password -> password

  def proxyPort(port: Int): Unit =
    props += proxy.outgoing.Port -> port

  def proxySslPort(port: Int): Unit =
    props += proxy.outgoing.SslPort -> port

  def httpsMode(mode: String): Unit =
    props += proxy.https.Mode -> mode

  def keystorePath(path: String): Unit =
    props += proxy.https.keyStore.Path -> path

  def keyStorePassword(password: String): Unit =
    props += proxy.https.keyStore.Password -> password

  def keyStoreType(keyStoreType: String): Unit =
    props += proxy.https.keyStore.Type -> keyStoreType

  def certificatePath(path: String): Unit =
    props += proxy.https.certificateAuthority.CertificatePath -> path

  def privateKeyPath(path: String): Unit =
    props += proxy.https.certificateAuthority.PrivateKeyPath -> path

  def build = props
}
