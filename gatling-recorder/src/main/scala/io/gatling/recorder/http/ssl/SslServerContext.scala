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

import java.io.{ FileInputStream, InputStream, File }
import java.nio.file.Path
import java.security.{ Security, KeyStore }
import javax.net.ssl.{ SSLEngine, X509KeyManager, KeyManagerFactory, SSLContext }

import scala.collection.concurrent.TrieMap
import scala.util.{ Failure, Try }

import io.gatling.commons.util.Io._
import io.gatling.commons.util.PathHelper._
import io.gatling.recorder.config.RecorderConfiguration

private[http] sealed trait SslServerContext {

  def password: Array[Char]

  def keyStore: KeyStore

  def context(alias: String): SSLContext

  def createSSLEngine(alias: String): SSLEngine = {
    val engine = context(alias).createSSLEngine
    engine.setUseClientMode(false)
    engine
  }
}

private[recorder] object SslServerContext {

  val GatlingSelfSignedKeyStore = "gatling.jks"
  val GatlingKeyStoreType = KeyStoreType.JKS
  val GatlingPassword = "gatling"
  val GatlingCAKeyFile = "gatlingCA.key.pem"
  val GatlingCACrtFile = "gatlingCA.cert.pem"
  val Algorithm = Option(Security.getProperty("ssl.KeyManagerFactory.algorithm")).getOrElse("SunX509")
  val Protocol = "TLS"

  def apply(config: RecorderConfiguration): SslServerContext = {

    import config.proxy.https._

    mode match {
      case HttpsMode.SelfSignedCertificate => SelfSignedCertificate

      case HttpsMode.ProvidedKeyStore =>
        val ksFile = new File(keyStore.path)
        val keyStoreType = keyStore.keyStoreType
        val password = keyStore.password.toCharArray
        new ProvidedKeystore(ksFile, keyStoreType, password)

      case HttpsMode.CertificateAuthority =>
        new CertificateAuthority(certificateAuthority.certificatePath, certificateAuthority.privateKeyPath)
    }
  }

  abstract class ImmutableFactory extends SslServerContext {

    def keyStoreInitStream: InputStream
    def keyStoreType: KeyStoreType

    lazy val keyStore = {
      val ks = KeyStore.getInstance(keyStoreType.toString)
      withCloseable(keyStoreInitStream) { ks.load(_, password) }
      ks
    }

    lazy val context = {
      // Set up key manager factory to use our key store
      val kmf = KeyManagerFactory.getInstance(Algorithm)
      kmf.init(keyStore, password)

      // Initialize the SSLContext to work with our key managers.
      val serverContext = SSLContext.getInstance(Protocol)
      serverContext.init(kmf.getKeyManagers, null, null)

      serverContext
    }

    def context(alias: String): SSLContext = context
  }

  object SelfSignedCertificate extends ImmutableFactory {

    def keyStoreInitStream: InputStream = classpathResourceAsStream(GatlingSelfSignedKeyStore)
    val keyStoreType = GatlingKeyStoreType

    val password: Array[Char] = GatlingPassword.toCharArray
  }

  class ProvidedKeystore(ksFile: File, val keyStoreType: KeyStoreType, val password: Array[Char]) extends ImmutableFactory {

    def keyStoreInitStream: InputStream = new FileInputStream(ksFile)
  }

  abstract class OnTheFlyFactory extends SslServerContext {

    val aliasContexts = TrieMap.empty[String, SSLContext]

    def context(alias: String): SSLContext = synchronized {
      aliasContexts.getOrElseUpdate(alias, newAliasContext(alias))
    }

    def ca(): Try[Ca]

    private def newAliasContext(alias: String): SSLContext =
      SslCertUtil.updateKeystoreWithNewAlias(keyStore, password, alias, ca) match {
        case Failure(t) => throw t
        case _ =>
          // Set up key manager factory to use our key store
          val kmf = KeyManagerFactory.getInstance(Algorithm)
          kmf.init(keyStore, password)

          // Initialize the SSLContext to work with our key manager
          val serverContext = SSLContext.getInstance(Protocol)
          serverContext.init(Array(new KeyManagerDelegate(kmf.getKeyManagers.head.asInstanceOf[X509KeyManager], alias)), null, null)
          serverContext
      }

    val password: Array[Char] = GatlingPassword.toCharArray

    lazy val keyStore = {
      val ks = KeyStore.getInstance(GatlingKeyStoreType.toString)
      ks.load(null, null)
      ks
    }
  }

  case class CertificateAuthority(pemCrtFile: Path, pemKeyFile: Path) extends OnTheFlyFactory {

    require(pemCrtFile.isFile, s"$pemCrtFile is not a file")
    require(pemKeyFile.isFile, s"$pemKeyFile is not a file")

    lazy val ca = SslCertUtil.getCA(pemCrtFile.inputStream, pemKeyFile.inputStream)
  }
}
