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

package io.gatling.commons.shared.unstable.util

import java.io.{ File, FileNotFoundException }

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class SslSpec extends AnyFlatSpecLike with Matchers {

  private val keystore = "testkeystore"
  private val password = "123456"

  private val classLoader = this.getClass.getClassLoader

  private def fileFromResource(classPathResource: String) =
    new File(classLoader.getResource(classPathResource).getFile).getCanonicalPath

  "SSLHelperSpec" should "load keystore from file" in {
    val keystoreFile = fileFromResource(keystore)

    val keyManagers = Ssl.newKeyManagerFactory(None, keystoreFile, password, None).getKeyManagers
    keyManagers should have size 1
  }

  it should "load keystore from classpath" in {
    val keyManagers = Ssl.newKeyManagerFactory(None, keystore, password, None).getKeyManagers
    keyManagers should have size 1
  }

  it should "throw FileNotFoundException when load non-existing keystore from classpath" in {
    a[FileNotFoundException] shouldBe thrownBy(Ssl.newKeyManagerFactory(None, "some/non/existing", password, None))
  }

  it should "load truststore from file" in {
    val truststoreFile = fileFromResource(keystore)

    val trustManagers = Ssl.newTrustManagerFactory(None, truststoreFile, password, None).getTrustManagers
    trustManagers should have size 1
  }

  it should "load truststore from classpath" in {
    val trustManagers = Ssl.newTrustManagerFactory(None, keystore, password, None).getTrustManagers
    trustManagers should have size 1
  }

  it should "throw FileNotFoundException when load non-existing truststore from classpath" in {
    a[FileNotFoundException] shouldBe thrownBy(Ssl.newTrustManagerFactory(None, "some/non/existing", password, None))
  }
}
