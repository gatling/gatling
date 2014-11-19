package io.gatling.recorder.http.ssl

import io.gatling.core.util.ClassSimpleNameToString
import io.gatling.recorder.util.Labelled

sealed abstract class HttpsMode(val label: String) extends Labelled with ClassSimpleNameToString

case object HttpsMode {

  case object SelfSignedCertificate extends HttpsMode("Self-signed Certificate")
  case object ProvidedKeyStore extends HttpsMode("Provided Keystore")
  case object CertificateAuthority extends HttpsMode("Certificate Authority")

  val AllHttpsModes = List(
    SelfSignedCertificate,
    ProvidedKeyStore,
    CertificateAuthority)

  def apply(s: String): HttpsMode =
    AllHttpsModes.find(_.toString == s).getOrElse {
      throw new IllegalArgumentException(s"$s is not a valid HTTPS mode")
    }
}
