/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

private[gatling] object CommandLineConstants {

  val Help = new CommandLineConstant("help", "h", "Show help (this message) and exit", None)
  val LocalPort = new CommandLineConstant("local-port", "lp", "Local port used by Gatling Proxy for HTTP/HTTPS", None)
  val ProxyHost = new CommandLineConstant("proxy-host", "ph", "Outgoing proxy host", Some("<host>"))
  val ProxyPort = new CommandLineConstant("proxy-port", "pp", "Outgoing proxy port for HTTP", Some("<port>"))
  val ProxyPortSsl = new CommandLineConstant("proxy-port-ssl", "pps", "Outgoing proxy port for HTTPS", Some("<port>"))
  val SimulationsFolder = new CommandLineConstant(
    "simulations-folder",
    "sf",
    "Uses <directoryPath> as the absolute path of the directory where simulations are stored",
    Some("<directoryPath>")
  )
  val ResourcesFolder =
    new CommandLineConstant("resources-folder", "rf", "Uses <folderName> as the folder where generated resources will be stored", Some("<folderName>"))
  val ClassName = new CommandLineConstant("class-name", "cn", "Sets the name of the generated class", Some("<className>"))
  val Package = new CommandLineConstant("package", "pkg", "Sets the package of the generated class", Some("<package>"))
  val Encoding = new CommandLineConstant("encoding", "enc", "Sets the encoding used in the recorder", Some("<encoding>"))
  val FollowRedirect = new CommandLineConstant("follow-redirect", "fr", """Sets the "Follow Redirects" option to true""", None)
  val AutomaticReferer = new CommandLineConstant("automatic-referer", "ar", """Sets the "Automatic Referers" option to true""", None)
  val InferHtmlResources = new CommandLineConstant("infer-html-resources", "ihr", """Sets the "Fetch html resources" option to true""", None)
  val Mode = new CommandLineConstant("mode", "m", "The Recorder mode to use", None)
  val Headless = new CommandLineConstant("headless", "cli", "Run the Recorder in headless mode", None)
  val HarFilePath = new CommandLineConstant("har-file", "hf", "The path of the HAR file to convert", Some("<HarFilePath>"))
}
