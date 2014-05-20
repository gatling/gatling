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
package io.gatling.recorder

case class CommandLineConstant(full: String, abbr: String)

object CommandLineConstants {

  val Help = CommandLineConstant("help", "h")
  val LocalPort = CommandLineConstant("local-port", "lp")
  val LocalPortSsl = CommandLineConstant("local-port-ssl", "lps")
  val ProxyHost = CommandLineConstant("proxy-host", "ph")
  val ProxyPort = CommandLineConstant("proxy-port", "pp")
  val ProxyPortSsl = CommandLineConstant("proxy-port-ssl", "pps")
  val OutputFolder = CommandLineConstant("output-folder", "of")
  val RequestBodiesFolder = CommandLineConstant("request-bodies-folder", "rbf")
  val ClassName = CommandLineConstant("class-name", "cn")
  val Package = CommandLineConstant("package", "pkg")
  val Encoding = CommandLineConstant("encoding", "enc")
  val FollowRedirect = CommandLineConstant("follow-redirect", "fr")
  val AutomaticReferer = CommandLineConstant("automatic-referer", "ar")
  val FetchHtmlResources = CommandLineConstant("fetch-html-resources", "fhr")
}
