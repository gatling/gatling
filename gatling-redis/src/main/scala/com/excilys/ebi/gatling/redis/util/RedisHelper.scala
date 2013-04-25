/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package com.excilys.ebi.gatling.core.feeder.redis.util

object RedisHelper {

	/* 
    Generate Redis protocol required for mass insert
    i.e  generateRedisProtocol("LPUSH", "SIM", "SOMETHING COOL!")
  */
	def generateRedisProtocol(d: String*): String = {
		val length = d.toList.length
		val protocol = new StringBuilder().append("*").append(length).append("\r\n")
		d.toList map { x => protocol.append("$").append(x.length).append("\r\n").append(x).append("\r\n") }
		protocol.toString
	}
}
