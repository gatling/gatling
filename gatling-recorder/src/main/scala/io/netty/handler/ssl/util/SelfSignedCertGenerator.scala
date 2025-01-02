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

package io.netty.handler.ssl.util

import java.io.File
import java.math.BigInteger
import java.security.{ KeyPairGenerator, NoSuchAlgorithmException }
import java.util.Date

import io.gatling.recorder.internal.bouncycastle.asn1.x500.X500Name
import io.gatling.recorder.internal.bouncycastle.cert.jcajce.{ JcaX509CertificateConverter, JcaX509v3CertificateBuilder }
import io.gatling.recorder.internal.bouncycastle.jce.provider.BouncyCastleProvider
import io.gatling.recorder.internal.bouncycastle.operator.jcajce.JcaContentSignerBuilder

object SelfSignedCertGenerator {

  val BcProvider: BouncyCastleProvider = new BouncyCastleProvider()

  def generate(): (File, File) = {
    val fqdn = "localhost"
    val random = ThreadLocalInsecureRandom.current

    val keypair =
      try {
        val keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, random);
        keyGen.generateKeyPair();
      } catch {
        // Should not reach here because every Java implementation must have RSA and EC key pair generator.
        case e: NoSuchAlgorithmException => throw new Error(e);
      }

    val privateKey = keypair.getPrivate

    // Prepare the information required for generating an X.509 certificate.
    val owner = new X500Name(s"CN=$fqdn")
    val builder = new JcaX509v3CertificateBuilder(
      owner,
      new BigInteger(64, random),
      new Date(System.currentTimeMillis() - 86400000L * 365),
      new Date(253402300799000L),
      owner,
      keypair.getPublic
    )

    val signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(privateKey)
    val certHolder = builder.build(signer)
    val cert = new JcaX509CertificateConverter().setProvider(BcProvider).getCertificate(certHolder)
    cert.verify(keypair.getPublic)

    val Array(certificatePath, privateKeyPath) = SelfSignedCertificate.newSelfSignedCertificate(fqdn, privateKey, cert)

    (new File(certificatePath), new File(privateKeyPath))
  }
}
