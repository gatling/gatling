/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 * Copyright 2012 Gilt Groupe, Inc. (www.gilt.com)
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
package io.gatling.http.action

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.Protocols
import io.gatling.http.config.HttpProtocol

abstract class HttpActionBuilder extends ActionBuilder {

  def httpProtocol(protocols: Protocols) = protocols.getProtocol[HttpProtocol].getOrElse(throw new UnsupportedOperationException("Http Protocol wasn't registered"))
  override def registerDefaultProtocols(protocols: Protocols): Protocols = protocols + HttpProtocol.DefaultHttpProtocol
}
