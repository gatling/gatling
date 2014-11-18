package io.gatling.recorder.http.ssl

import io.gatling.core.util.ClassSimpleNameToString
import io.gatling.recorder.util.Labelled

sealed abstract class KeyStoreType(val label: String) extends Labelled with ClassSimpleNameToString

object KeyStoreType {

  case object JKS extends KeyStoreType("JKS")
  case object PKCS12 extends KeyStoreType("PKCS#12")

  val AllKeyStoreTypes = List(JKS, PKCS12)

  def apply(s: String) = AllKeyStoreTypes.find(_.toString == s).getOrElse {
    throw new IllegalArgumentException(s"$s is not a valid keystore type")
  }
}
