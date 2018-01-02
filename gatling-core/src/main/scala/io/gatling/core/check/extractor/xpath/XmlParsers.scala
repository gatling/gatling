/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.core.check.extractor.xpath

import io.gatling.core.config.GatlingConfiguration

import org.xml.sax.InputSource

class XmlParsers(implicit configuration: GatlingConfiguration) {

  val saxon = new Saxon(configuration)
  val jdk = new JdkXmlParsers(configuration)

  val parse: InputSource => Dom =
    if (saxon.enabled)
      is => SaxonDom(saxon.parse(is))
    else
      is => JdkDom(jdk.parse(is))
}
