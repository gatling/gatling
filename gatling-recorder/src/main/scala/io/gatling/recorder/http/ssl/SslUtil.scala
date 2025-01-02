/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
import java.nio.file.{ Files, Path }
import java.security._
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.ThreadLocalRandom
import javax.security.auth.x500.X500Principal

import scala.concurrent.duration._
import scala.util.{ Try, Using }

import io.gatling.recorder.internal.bouncycastle.asn1.x509.{ Extension, GeneralName, GeneralNames }
import io.gatling.recorder.internal.bouncycastle.cert.{ X509CertificateHolder, X509v3CertificateBuilder }
import io.gatling.recorder.internal.bouncycastle.cert.jcajce.{ JcaX509CertificateConverter, JcaX509CertificateHolder, JcaX509v1CertificateBuilder }
import io.gatling.recorder.internal.bouncycastle.jce.provider.BouncyCastleProvider
import io.gatling.recorder.internal.bouncycastle.openssl.{ PEMKeyPair, PEMParser }
import io.gatling.recorder.internal.bouncycastle.openssl.jcajce.{ JcaPEMKeyConverter, JcaPEMWriter }
import io.gatling.recorder.internal.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import io.gatling.recorder.internal.bouncycastle.pkcs.PKCS10CertificationRequest
import io.gatling.recorder.internal.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.ssl.{ OpenSsl, SslProvider }
import io.netty.handler.ssl.util.SelfSignedCertGenerator

private[ssl] final case class Ca(cert: X509Certificate, privKey: PrivateKey)
private[ssl] final case class Csr(cert: PKCS10CertificationRequest, privKey: PrivateKey, hostname: String)

/**
 * Utility class to create SSL server certificate on the fly for the recorder keystore
 */
private[recorder] object SslUtil extends StrictLogging {

  Security.addProvider(SelfSignedCertGenerator.BcProvider)

  private[ssl] val TheSslProvider =
    if (OpenSsl.isAvailable) {
      logger.info("OpenSSL is not available on your architecture.")
      SslProvider.OPENSSL
    } else {
      SslProvider.JDK
    }

  def generateSelfSignedCertificate(): (File, File) = SelfSignedCertGenerator.generate()

  def readPEM(file: InputStream): Any =
    Using.resource(new PEMParser(new InputStreamReader(file)))(_.readObject)

  def writePEM(obj: Any, os: OutputStream): Unit =
    Using.resource(new JcaPEMWriter(new OutputStreamWriter(os)))(_.writeObject(obj))

  def certificateFromHolder(certHolder: X509CertificateHolder): X509Certificate =
    new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certHolder)

  def newRSAKeyPair: KeyPair = {
    val kpGen = KeyPairGenerator.getInstance("RSA")
    kpGen.initialize(1024)
    kpGen.generateKeyPair
  }

  private def newSigner(privKey: PrivateKey) = new JcaContentSignerBuilder("SHA256withRSA").build(privKey)

  def generateGatlingCAPEMFiles(dir: Path, privKeyFileName: String, certFileName: String): Unit = {
    assert(Files.isDirectory(dir), s"$dir isn't a directory")

    def generateCACertificate(pair: KeyPair): X509CertificateHolder = {
      val dn = s"C=FR, ST=Val de marne, O=GatlingCA, CN=Gatling"
      val now = System.currentTimeMillis()

      // has to be v1 for CA
      val certGen = new JcaX509v1CertificateBuilder(
        new X500Principal(dn), // issuer
        BigInteger.valueOf(now), // serial
        new Date(now), // notBefore
        new Date(now + 365.days.toMillis), // notAfter
        new X500Principal(dn), // subject
        pair.getPublic
      ) // publicKey

      val signer = newSigner(pair.getPrivate)
      certGen.build(signer)
    }

    val pair = newRSAKeyPair
    val crtHolder = generateCACertificate(pair)

    writePEM(crtHolder, new BufferedOutputStream(Files.newOutputStream(dir.resolve(certFileName))))
    writePEM(pair, new BufferedOutputStream(Files.newOutputStream(dir.resolve(privKeyFileName))))
  }

  def getCA(crtFile: InputStream, keyFile: InputStream): Try[Ca] =
    Try {
      val certificate = readPEM(crtFile) match {
        case x509: X509CertificateHolder => certificateFromHolder(x509)
        case _                           => throw new IllegalArgumentException("Cert file is not a valid X509 cert")
      }

      val privKey = readPEM(keyFile) match {
        case pem: PEMKeyPair => new JcaPEMKeyConverter().getPrivateKey(pem.getPrivateKeyInfo)
        case _               => throw new IllegalArgumentException("Key file is not a valid PEM key pair")
      }

      Ca(certificate, privKey)
    }

  def updateKeystoreWithNewAlias(keyStore: KeyStore, password: Array[Char], alias: String, caT: Try[Ca]): Try[KeyStore] =
    for {
      ca <- caT
      csr <- createCSR(alias)
      serverCrt <- createServerCert(ca.cert, ca.privKey, csr.cert, csr.hostname)
      updatedKeyStore <- addNewKeystoreEntry(keyStore, password, serverCrt, csr.privKey, ca.cert, alias)
    } yield updatedKeyStore

  private def createCSR(hostname: String): Try[Csr] =
    Try {
      val pair = newRSAKeyPair
      val dn = s"C=FR, ST=Val de marne, O=GatlingCA, OU=Gatling, CN=$hostname"
      val builder = new JcaPKCS10CertificationRequestBuilder(new X500Principal(dn), pair.getPublic)
      val signer = newSigner(pair.getPrivate)

      val pkcs10CR = builder.build(signer)
      Csr(pkcs10CR, pair.getPrivate, hostname)
    }

  private def createServerCert(caCert: X509Certificate, caKey: PrivateKey, csr: PKCS10CertificationRequest, hostname: String): Try[X509Certificate] =
    Try {
      val now = System.currentTimeMillis()
      val certBuilder = new X509v3CertificateBuilder(
        new JcaX509CertificateHolder(caCert).getSubject, // issuer
        BigInteger.valueOf(ThreadLocalRandom.current.nextLong), // serial
        new Date(now), // notBefore
        new Date(now + 1.day.toMillis), // notAfter
        csr.getSubject, // subject
        csr.getSubjectPublicKeyInfo
      ) // publicKey

      val subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, hostname)).getEncoded()
      certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltName)

      val signer = newSigner(caKey)
      certificateFromHolder(certBuilder.build(signer))
    }

  private def addNewKeystoreEntry(
      keyStore: KeyStore,
      password: Array[Char],
      serverCert: X509Certificate,
      csrPrivKey: PrivateKey,
      caCert: X509Certificate,
      alias: String
  ): Try[KeyStore] =
    Try {
      keyStore.setCertificateEntry(alias, serverCert)
      keyStore.setKeyEntry(alias, csrPrivKey, password, Array(serverCert, caCert))
      keyStore
    }
}
