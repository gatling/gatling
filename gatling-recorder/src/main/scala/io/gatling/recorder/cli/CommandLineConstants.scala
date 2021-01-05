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

package io.gatling.recorder.cli

import io.gatling.core.cli.CommandLineConstant

private[cli] object CommandLineConstants {

  val Help = new CommandLineConstant("help", "h")
  val LocalPort = new CommandLineConstant("local-port", "lp")
  val ProxyHost = new CommandLineConstant("proxy-host", "ph")
  val ProxyPort = new CommandLineConstant("proxy-port", "pp")
  val ProxyPortSsl = new CommandLineConstant("proxy-port-ssl", "pps")
  val SimulationsFolder = new CommandLineConstant("simulations-folder", "sf")
  val ResourcesFolder = new CommandLineConstant("resources-folder", "rf")
  val ClassName = new CommandLineConstant("class-name", "cn")
  val Package = new CommandLineConstant("package", "pkg")
  val Encoding = new CommandLineConstant("encoding", "enc")
  val FollowRedirect = new CommandLineConstant("follow-redirect", "fr")
  val AutomaticReferer = new CommandLineConstant("automatic-referer", "ar")
  val InferHtmlResources = new CommandLineConstant("infer-html-resources", "ihr")
  val Mode = new CommandLineConstant("mode", "m")
  val Headless = new CommandLineConstant("headless", "cli")
  val HarFilePath = new CommandLineConstant("har-file", "hf")
}
