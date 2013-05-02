/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.util

import java.io.{ File, FileInputStream }
import java.security.{ KeyStore, SecureRandom }
import com.ning.http.client.AsyncHttpClientConfig
import com.excilys.ebi.gatling.core.util.IOHelper.use
import javax.net.ssl.{ KeyManager, KeyManagerFactory, SSLContext, TrustManager, TrustManagerFactory }

object SSLHelper {

	def newTrustManagers(storeType: String, file: String, password: String, algorithm: Option[String]): Array[TrustManager] = {

		use(new FileInputStream(new File(file))) { is =>
			val trustStore = KeyStore.getInstance(storeType)
			trustStore.load(is, password.toCharArray)
			val algo = algorithm.getOrElse(KeyManagerFactory.getDefaultAlgorithm)
			val tmf = TrustManagerFactory.getInstance(algo)
			tmf.init(trustStore)
			tmf.getTrustManagers
		}
	}

	def newKeyManagers(storeType: String, file: String, password: String, algorithm: Option[String]): Array[KeyManager] = {

		use(new FileInputStream(new File(file))) { is =>
			val keyStore = KeyStore.getInstance(storeType)
			val passwordCharArray = password.toCharArray
			keyStore.load(is, passwordCharArray)
			val algo = algorithm.getOrElse(KeyManagerFactory.getDefaultAlgorithm)
			val kmf = KeyManagerFactory.getInstance(algo)
			kmf.init(keyStore, passwordCharArray)
			kmf.getKeyManagers
		}
	}

	def setSSLContext(ahcConfigBuilder: AsyncHttpClientConfig.Builder, trustManagers: Option[Array[TrustManager]], keyManagers: Option[Array[KeyManager]]): AsyncHttpClientConfig.Builder = {
		val sslContext = SSLContext.getInstance("TLS")
		sslContext.init(keyManagers.getOrElse(null), trustManagers.getOrElse(null), new SecureRandom)
		ahcConfigBuilder.setSSLContext(sslContext)
	}
}