/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.core.util

import java.io.{ BufferedOutputStream, File, FileNotFoundException, FileOutputStream }
import java.security.KeyStore
import java.security.cert.Certificate
import javax.net.ssl.{ KeyManagerFactory, X509KeyManager }

import scala.jdk.CollectionConverters._
import scala.util.Using

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class PerUserKeyManagerFactorySpec extends AnyFlatSpecLike with Matchers {
  // keyStore generated with keytool, contains the key entries "user-01" to "user-08"
  private val KeyStorePath = "per-user-keystore.p12"
  private val KeyStorePassword = "gatling"
  private val KeyStoreEntries = 8

  private def certificateOf(kmf: KeyManagerFactory): Certificate = {
    val keyManager = kmf.getKeyManagers.head.asInstanceOf[X509KeyManager]
    val aliases = keyManager.getClientAliases("EC", null)
    aliases should have length 1
    keyManager.getCertificateChain(aliases.head).head
  }

  private def certificatesOf(userIds: Seq[Long]): Seq[Certificate] =
    certificatesOf(KeyStorePath, Some(KeyStorePassword), userIds)

  private def certificatesOf(path: String, password: Option[String], userIds: Seq[Long]): Seq[Certificate] = {
    val f = PerUserKeyManagerFactory.fromKeyStore0(path, password)
    userIds.map(userId => certificateOf(f(userId)))
  }

  private def loadKeyStore(path: String, password: String): KeyStore =
    Using.resource(Resource.resolveResource(path).toOption.get.inputStream) { is =>
      val keyStore = KeyStore.getInstance("PKCS12")
      keyStore.load(is, password.toCharArray)
      keyStore
    }

  "fromKeyStore" should "assign a distinct key entry to each virtual user" in {
    val certificates = certificatesOf(1L to KeyStoreEntries.toLong)
    certificates.distinct should have length KeyStoreEntries.toLong
  }

  it should "assign the key entries in the aliases' natural order" in {
    val keyStore = loadKeyStore(KeyStorePath, KeyStorePassword)
    val expected = keyStore.aliases.asScala.toSeq.sorted.map(keyStore.getCertificate)

    certificatesOf(1L to KeyStoreEntries.toLong) shouldBe expected
  }

  it should "support a keyStore that's not password protected" in {
    // keytool can't generate one, the store password must be at least 6 characters long
    val keyStore = loadKeyStore(KeyStorePath, KeyStorePassword)
    val protection = new KeyStore.PasswordProtection(KeyStorePassword.toCharArray)
    val noPasswordKeyStore = KeyStore.getInstance("PKCS12")
    noPasswordKeyStore.load(null, Array.emptyCharArray)
    keyStore.aliases.asScala.foreach(alias =>
      noPasswordKeyStore.setEntry(alias, keyStore.getEntry(alias, protection), new KeyStore.PasswordProtection(Array.emptyCharArray))
    )

    val file = File.createTempFile("gatling-per-user-keystore", ".p12")
    file.deleteOnExit()
    Using.resource(new BufferedOutputStream(new FileOutputStream(file))) { os =>
      noPasswordKeyStore.store(os, Array.emptyCharArray)
    }

    certificatesOf(file.getAbsolutePath, None, 1L to KeyStoreEntries.toLong) shouldBe
      certificatesOf(1L to KeyStoreEntries.toLong)
  }

  it should "fail when there are more virtual users than key entries" in {
    a[PerUserKeyManagerFactory.ExhaustedKeyStoreException] should be thrownBy
      certificatesOf(1L to (KeyStoreEntries + 1).toLong)
  }

  it should "fail when the keyStore can't be found" in {
    a[FileNotFoundException] should be thrownBy
      PerUserKeyManagerFactory.fromKeyStore0("missing-keystore.p12", Some(KeyStorePassword))
  }
}
