/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

package io.gatling.commons.util

import java.io.{ File, FileInputStream, FileNotFoundException, InputStream }
import java.security.KeyStore
import javax.net.ssl.{ KeyManagerFactory, TrustManagerFactory }

import io.gatling.commons.util.Io.withCloseable

object Ssl {

  private def storeStream(filePath: String): InputStream = {
    val storeFile = new File(filePath)
    if (storeFile.exists)
      new FileInputStream(storeFile)
    else
      Option(getClass.getClassLoader.getResourceAsStream(filePath)).getOrElse(throw new FileNotFoundException(filePath))
  }

  def newTrustManagerFactory(storeType: Option[String], file: String, password: String, algorithm: Option[String]): TrustManagerFactory =
    withCloseable(storeStream(file)) { is =>
      val trustStore = KeyStore.getInstance(storeType.getOrElse(KeyStore.getDefaultType))
      trustStore.load(is, password.toCharArray)
      val algo = algorithm.getOrElse(KeyManagerFactory.getDefaultAlgorithm)
      val tmf = TrustManagerFactory.getInstance(algo)
      tmf.init(trustStore)
      tmf
    }

  def newKeyManagerFactory(storeType: Option[String], file: String, password: String, algorithm: Option[String]): KeyManagerFactory =
    withCloseable(storeStream(file)) { is =>
      val keyStore = KeyStore.getInstance(storeType.getOrElse(KeyStore.getDefaultType))
      val passwordCharArray = password.toCharArray
      keyStore.load(is, passwordCharArray)
      val algo = algorithm.getOrElse(KeyManagerFactory.getDefaultAlgorithm)
      val kmf = KeyManagerFactory.getInstance(algo)
      kmf.init(keyStore, passwordCharArray)
      kmf
    }
}
