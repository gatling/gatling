/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.recorder.config

import scala.beans.BeanProperty

class ProxyConfig(@BeanProperty var host: Option[String] = None, @BeanProperty var port: Option[Int] = None, @BeanProperty var sslPort: Option[Int] = None, @BeanProperty var username: Option[String] = None, @BeanProperty var password: Option[String] = None) {
	override def toString =
		new StringBuilder("ProxyConfig [")
			.append("host=").append(host).append(", ")
			.append("port=").append(port).append(", ")
			.append("sslPort=").append(sslPort)
			.append("username=").append(username).append(", ")
			.append("password=").append(password).append(", ")
			.append("]")
			.toString
}
