/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.security.{ KeyStore, Provider }
import javax.net.ssl._

object KeyManagerFactoryDelegate {

  def apply(factory: KeyManagerFactory, alias: String): KeyManagerFactoryDelegate = {
    val spi = new KeyManagerFactorySpi {
      override def engineInit(ks: KeyStore, password: Array[Char]): Unit =
        factory.init(ks, password)

      override def engineInit(spec: ManagerFactoryParameters): Unit =
        factory.init(spec)

      override def engineGetKeyManagers(): Array[KeyManager] =
        Array(new KeyManagerDelegate(factory.getKeyManagers.head.asInstanceOf[X509KeyManager], alias))
    }

    new KeyManagerFactoryDelegate(spi, factory.getProvider, factory.getAlgorithm)
  }
}

class KeyManagerFactoryDelegate(spi: KeyManagerFactorySpi, provider: Provider, algorithm: String) extends KeyManagerFactory(spi, provider, algorithm)
