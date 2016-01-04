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
package io.gatling.http.util

import java.io.{ FileNotFoundException, File }

import io.gatling.BaseSpec

class SSLHelperSpec extends BaseSpec {

  val KEYSTORE = "testkeystore"
  val PASSWORD = "123456"

  val classLoader = this.getClass.getClassLoader

  def fileFromResource(classPathResource: String): String = {
    new File(classLoader.getResource(classPathResource).getFile).getCanonicalPath
  }

  "SSLHelperSpec" should "load keystore from file" in {
    val keystoreFile = fileFromResource(KEYSTORE)

    val keyManagers = SslHelper.newKeyManagerFactory(None, keystoreFile, PASSWORD, None).getKeyManagers
    keyManagers should have size 1
  }

  it should "load keystore from classpath" in {
    val keyManagers = SslHelper.newKeyManagerFactory(None, KEYSTORE, PASSWORD, None).getKeyManagers
    keyManagers should have size 1
  }

  it should "throw FileNotFoundException when load non-existing keystore from classpath" in {
    a[FileNotFoundException] shouldBe thrownBy(SslHelper.newKeyManagerFactory(None, "some/non/existing", PASSWORD, None))
  }

  it should "load truststore from file" in {
    val truststoreFile = fileFromResource(KEYSTORE)

    val trustManagers = SslHelper.newTrustManagerFactory(None, truststoreFile, PASSWORD, None).getTrustManagers
    trustManagers should have size 1
  }

  it should "load truststore from classpath" in {
    val trustManagers = SslHelper.newTrustManagerFactory(None, KEYSTORE, PASSWORD, None).getTrustManagers
    trustManagers should have size 1
  }

  it should "throw FileNotFoundException when load non-existing truststore from classpath" in {
    a[FileNotFoundException] shouldBe thrownBy(SslHelper.newTrustManagerFactory(None, "some/non/existing", PASSWORD, None))
  }
}
