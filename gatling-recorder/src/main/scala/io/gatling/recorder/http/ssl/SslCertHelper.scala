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

import java.io._
import java.math.BigInteger
import java.nio.file.Path
import java.security._
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal

import scala.util.Try
import scala.concurrent.duration._

import io.gatling.commons.util.Io.withCloseable
import io.gatling.commons.util.PathHelper._
import io.gatling.commons.util.TimeHelper._

import com.typesafe.scalalogging.StrictLogging
import org.bouncycastle.cert.{ X509CertificateHolder, X509v3CertificateBuilder }
import org.bouncycastle.cert.jcajce.{ JcaX509CertificateConverter, JcaX509CertificateHolder, JcaX509v1CertificateBuilder }
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.{ PEMKeyPair, PEMParser }
import org.bouncycastle.openssl.jcajce.{ JcaPEMKeyConverter, JcaPEMWriter }
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder

private[ssl] case class Ca(cert: X509Certificate, privKey: PrivateKey)
private[ssl] case class Csr(cert: PKCS10CertificationRequest, privKey: PrivateKey)

/**
 * Utility class to create SSL server certificate on the fly for the recorder keystore
 */
private[recorder] object SslCertUtil extends StrictLogging {

  Security.addProvider(new BouncyCastleProvider)

  def readPEM(file: InputStream): Any = withCloseable(new PEMParser(new InputStreamReader(file))) { _.readObject }

  def writePEM(obj: Any, os: OutputStream): Unit = withCloseable(new JcaPEMWriter(new OutputStreamWriter(os))) { _.writeObject(obj) }

  def certificateFromHolder(certHolder: X509CertificateHolder) = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certHolder)

  def newRSAKeyPair: KeyPair = {
    val kpGen = KeyPairGenerator.getInstance("RSA")
    kpGen.initialize(1024)
    kpGen.generateKeyPair
  }

  private def newSigner(privKey: PrivateKey) = new JcaContentSignerBuilder("SHA256withRSA").build(privKey)

  def generateGatlingCAPEMFiles(dir: Path, privKeyFileName: String, certFileName: String): Unit = {
    assert(dir.isDirectory, s"$dir isn't a directory")

      def generateCACertificate(pair: KeyPair): X509CertificateHolder = {
        val dn = s"C=FR, ST=Val de marne, O=GatlingCA, CN=Gatling"
        val now = nowMillis

        // has to be v1 for CA
        val certGen = new JcaX509v1CertificateBuilder(
          new X500Principal(dn), // issuer
          BigInteger.valueOf(now), // serial
          new Date(now), // notBefore
          new Date(now + 365.days.toMillis), // notAfter
          new X500Principal(dn), //subject
          pair.getPublic
        ) // publicKey

        val signer = newSigner(pair.getPrivate)
        certGen.build(signer)
      }

    val pair = newRSAKeyPair
    val crtHolder = generateCACertificate(pair)

    writePEM(crtHolder, (dir / certFileName).outputStream)
    writePEM(pair, (dir / privKeyFileName).outputStream)
  }

  def getCA(crtFile: InputStream, keyFile: InputStream): Try[Ca] =
    Try {
      val certHolder = readPEM(crtFile).asInstanceOf[X509CertificateHolder]
      val certificate = certificateFromHolder(certHolder)

      val keyInfo = readPEM(keyFile).asInstanceOf[PEMKeyPair].getPrivateKeyInfo
      val privKey = new JcaPEMKeyConverter().getPrivateKey(keyInfo)

      Ca(certificate, privKey)
    }

  def updateKeystoreWithNewAlias(keyStore: KeyStore, password: Array[Char], alias: String, caT: Try[Ca]): Try[KeyStore] =
    for {
      ca <- caT
      csr <- createCSR(alias)
      serverCrt <- createServerCert(ca.cert, ca.privKey, csr.cert)
      updatedKeyStore <- addNewKeystoreEntry(keyStore, password, serverCrt, csr.privKey, ca.cert, alias)
    } yield updatedKeyStore

  private def createCSR(dnHostName: String): Try[Csr] =
    Try {
      val pair = newRSAKeyPair
      val dn = s"C=FR, ST=Val de marne, O=GatlingCA, OU=Gatling, CN=$dnHostName"
      val builder = new JcaPKCS10CertificationRequestBuilder(new X500Principal(dn), pair.getPublic)
      val signer = newSigner(pair.getPrivate)
      val pkcs10CR = builder.build(signer)
      Csr(pkcs10CR, pair.getPrivate)
    }

  private def createServerCert(caCert: X509Certificate, caKey: PrivateKey, csr: PKCS10CertificationRequest): Try[X509Certificate] =
    Try {
      val now = nowMillis
      val certBuilder = new X509v3CertificateBuilder(
        new JcaX509CertificateHolder(caCert).getSubject, // issuer
        BigInteger.valueOf(now), // serial
        new Date(now), // notBefore
        new Date(now + 1.day.toMillis), // notAfter
        csr.getSubject, //subject
        csr.getSubjectPublicKeyInfo
      ) // publicKey
      val signer = newSigner(caKey)
      certificateFromHolder(certBuilder.build(signer))
    }

  private def addNewKeystoreEntry(keyStore: KeyStore, password: Array[Char], serverCert: X509Certificate, csrPrivKey: PrivateKey, caCert: X509Certificate, alias: String): Try[KeyStore] =
    Try {
      keyStore.setCertificateEntry(alias, serverCert)
      keyStore.setKeyEntry(alias, csrPrivKey, password, Array(serverCert, caCert))
      keyStore
    }
}
