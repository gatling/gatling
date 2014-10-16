package io.gatling.recorder.http.ssl

import io.gatling.core.util.ClassSimpleNameToString

sealed abstract class KeyStoreType(val name: String) extends ClassSimpleNameToString

object KeyStoreType {

  case object JKS extends KeyStoreType("JKS")
  case object PKCS12 extends KeyStoreType("PKCS12")

  def apply(s: String) = AllKeyStoreTypes.find(_.toString == s).getOrElse {
    throw new IllegalArgumentException(s"$s is not a valid keystore type")
  }
  val AllKeyStoreTypes = List(JKS, PKCS12)
}
