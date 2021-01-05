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

package io.gatling.recorder.config

import java.{ util => ju }

import scala.collection.mutable

import io.gatling.recorder.config.ConfigKeys._

class RecorderPropertiesBuilder {

  private val props = mutable.Map.empty[String, Any]

  def mode(mode: RecorderMode): RecorderPropertiesBuilder = {
    props += core.Mode -> mode.toString
    this
  }

  def encoding(encoding: String): RecorderPropertiesBuilder = {
    props += core.Encoding -> encoding
    this
  }

  def simulationsFolder(folder: String): RecorderPropertiesBuilder = {
    props += core.SimulationsFolder -> folder
    this
  }

  def resourcesFolder(folder: String): RecorderPropertiesBuilder = {
    props += core.ResourcesFolder -> folder
    this
  }

  def simulationPackage(pkg: String): RecorderPropertiesBuilder = {
    props += core.Package -> pkg
    this
  }

  def simulationClassName(className: String): RecorderPropertiesBuilder = {
    props += core.ClassName -> className
    this
  }

  def thresholdForPauseCreation(threshold: String): RecorderPropertiesBuilder = {
    props += core.ThresholdForPauseCreation -> threshold
    this
  }

  def saveConfig(status: Boolean): RecorderPropertiesBuilder = {
    props += core.SaveConfig -> status
    this
  }

  def headless(status: Boolean): RecorderPropertiesBuilder = {
    props += core.Headless -> status
    this
  }

  def harFilePath(path: String): RecorderPropertiesBuilder = {
    props += core.HarFilePath -> path
    this
  }

  def filterStrategy(strategy: String): RecorderPropertiesBuilder = {
    props += filters.FilterStrategy -> strategy
    this
  }

  def whitelist(patterns: ju.List[String]): RecorderPropertiesBuilder = {
    props += filters.WhitelistPatterns -> patterns
    this
  }

  def blacklist(patterns: ju.List[String]): RecorderPropertiesBuilder = {
    props += filters.BlacklistPatterns -> patterns
    this
  }

  def automaticReferer(status: Boolean): RecorderPropertiesBuilder = {
    props += http.AutomaticReferer -> status
    this
  }

  def followRedirect(status: Boolean): RecorderPropertiesBuilder = {
    props += http.FollowRedirect -> status
    this
  }

  def inferHtmlResources(status: Boolean): RecorderPropertiesBuilder = {
    props += http.InferHtmlResources -> status
    this
  }

  def removeCacheHeaders(status: Boolean): RecorderPropertiesBuilder = {
    props += http.RemoveCacheHeaders -> status
    this
  }

  def checkResponseBodies(status: Boolean): RecorderPropertiesBuilder = {
    props += http.CheckResponseBodies -> status
    this
  }

  def useSimulationAsPrefix(status: Boolean): RecorderPropertiesBuilder = {
    props += http.UseSimulationAsPrefix -> status
    this
  }

  def useMethodAndUriAsPostfix(status: Boolean): RecorderPropertiesBuilder = {
    props += http.UseMethodAndUriAsPostfix -> status
    this
  }

  def localPort(port: Int): RecorderPropertiesBuilder = {
    props += proxy.Port -> port
    this
  }

  def proxyHost(host: String): RecorderPropertiesBuilder = {
    props += proxy.outgoing.Host -> host
    this
  }

  def proxyUsername(username: String): RecorderPropertiesBuilder = {
    props += proxy.outgoing.Username -> username
    this
  }

  def proxyPassword(password: String): RecorderPropertiesBuilder = {
    props += proxy.outgoing.Password -> password
    this
  }

  def proxyPort(port: Int): RecorderPropertiesBuilder = {
    props += proxy.outgoing.Port -> port
    this
  }

  def proxySslPort(port: Int): RecorderPropertiesBuilder = {
    props += proxy.outgoing.SslPort -> port
    this
  }

  def httpsMode(mode: String): RecorderPropertiesBuilder = {
    props += proxy.https.Mode -> mode
    this
  }

  def keystorePath(path: String): RecorderPropertiesBuilder = {
    props += proxy.https.keyStore.Path -> path
    this
  }

  def keyStorePassword(password: String): RecorderPropertiesBuilder = {
    props += proxy.https.keyStore.Password -> password
    this
  }

  def keyStoreType(keyStoreType: String): RecorderPropertiesBuilder = {
    props += proxy.https.keyStore.Type -> keyStoreType
    this
  }

  def certificatePath(path: String): RecorderPropertiesBuilder = {
    props += proxy.https.certificateAuthority.CertificatePath -> path
    this
  }

  def privateKeyPath(path: String): RecorderPropertiesBuilder = {
    props += proxy.https.certificateAuthority.PrivateKeyPath -> path
    this
  }

  def build: mutable.Map[String, Any] = props
}
