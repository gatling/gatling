/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.example.securechat

import java.security.KeyStore
import java.security.cert.X509Certificate

import javax.net.ssl.{ ManagerFactoryParameters, TrustManager, TrustManagerFactorySpi, X509TrustManager }

object SecureChatTrustManagerFactory {

  val trustManagers = Array[TrustManager](new X509TrustManager {
    def getAcceptedIssuers = Array.empty[X509Certificate]

    def checkClientTrusted(chain: Array[X509Certificate], authType: String) {
      // Always trust
    }

    def checkServerTrusted(chain: Array[X509Certificate], authType: String) {
      // Always trust
    }
  })

}

/**
 * Bogus {@link TrustManagerFactorySpi} which accepts any certificate even if it
 * is invalid.
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 *
 * @version Rev: 2080 , Date: 2010-01-26 18:04:19 +0900 (Tue, 26 Jan 2010)
 */
class SecureChatTrustManagerFactory extends TrustManagerFactorySpi {

  def engineGetTrustManagers = SecureChatTrustManagerFactory.trustManagers

  def engineInit(keystore: KeyStore) {
    // Unused
  }

  def engineInit(managerFactoryParameters: ManagerFactoryParameters) {
    // Unused
  }
}