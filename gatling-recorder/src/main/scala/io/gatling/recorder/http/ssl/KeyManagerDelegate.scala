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
package io.gatling.recorder.http.ssl

import java.net.Socket
import java.security.{ PrivateKey, Principal }
import java.security.cert.X509Certificate
import javax.net.ssl.{ X509KeyManager, SSLEngine, X509ExtendedKeyManager }

import com.typesafe.scalalogging.StrictLogging

/**
 * Instructs to send server certificate according to the alias
 */
private[ssl] class KeyManagerDelegate(manager: X509KeyManager, alias: String) extends X509ExtendedKeyManager with StrictLogging {

  override def chooseEngineServerAlias(keyType: String, issuers: Array[Principal], engine: SSLEngine): String = alias

  override def getClientAliases(p1: String, p2: Array[Principal]): Array[String] = manager.getClientAliases(p1, p2)

  override def getPrivateKey(p1: String): PrivateKey = manager.getPrivateKey(p1)

  override def getCertificateChain(p1: String): Array[X509Certificate] = manager.getCertificateChain(p1)

  override def getServerAliases(p1: String, p2: Array[Principal]): Array[String] = manager.getServerAliases(p1, p2)

  override def chooseClientAlias(p1: Array[String], p2: Array[Principal], p3: Socket): String = manager.chooseClientAlias(p1, p2, p3)

  override def chooseServerAlias(p1: String, p2: Array[Principal], p3: Socket): String = manager.chooseServerAlias(p1, p2, p3)
}
