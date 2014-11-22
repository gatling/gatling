/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
