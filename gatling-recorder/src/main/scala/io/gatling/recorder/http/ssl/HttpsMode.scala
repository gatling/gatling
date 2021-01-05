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

import io.gatling.commons.util.ClassSimpleNameToString
import io.gatling.recorder.util.Labelled

private[recorder] sealed abstract class HttpsMode(val label: String) extends Labelled with ClassSimpleNameToString with Product with Serializable

private[recorder] case object HttpsMode {

  case object SelfSignedCertificate extends HttpsMode("Self-signed Certificate")
  case object ProvidedKeyStore extends HttpsMode("Provided Keystore")
  case object CertificateAuthority extends HttpsMode("Certificate Authority")

  val AllHttpsModes = List(
    SelfSignedCertificate,
    ProvidedKeyStore,
    CertificateAuthority
  )

  def apply(s: String): HttpsMode =
    AllHttpsModes.find(_.toString == s).getOrElse {
      throw new IllegalArgumentException(s"$s is not a valid HTTPS mode")
    }
}
