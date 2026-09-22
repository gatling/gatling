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

import java.io.FileNotFoundException
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory

import scala.jdk.CollectionConverters._
import scala.util.Using
import scala.util.control.NoStackTrace

import io.gatling.commons.validation.{ Failure, Success }

import com.typesafe.scalalogging.StrictLogging

private[gatling] object PerUserKeyManagerFactory extends StrictLogging {
  private val KeyStoreType = "PKCS12"

  /** Thrown when the keyStore doesn't contain enough key entries for all the virtual users of the run. */
  private[gatling] final class ExhaustedKeyStoreException(message: String) extends IllegalStateException(message) with NoStackTrace

  /**
   * Build a `KeyManagerFactory` per virtual user out of a single PKCS#12 keyStore that contains one key entry per virtual user. Entries are assigned in the
   * keyStore aliases' natural order, the virtual user with the userId 1 getting the first one. The returned function throws an [[ExhaustedKeyStoreException]]
   * once all the key entries have been assigned, so the run can be stopped instead of having 2 virtual users share the same key entry.
   *
   * When running on Gatling Enterprise with multiple load generators, the aliases are sharded so that each load generator gets its own disjoint slice, hence a
   * given key entry is never used by 2 different load generators.
   *
   * @param path
   *   the location of the PKCS#12 keyStore, either on the classpath or as an absolute path on the filesystem
   * @param password
   *   the keyStore password, also used to recover the key entries
   */
  def fromKeyStore(path: String, password: Option[String]): Long => KeyManagerFactory =
    fromKeyStore0(path, password)

  private[util] def fromKeyStore0(path: String, password: Option[String]): Long => KeyManagerFactory = {
    val resource = Resource.resolveResource(path) match {
      case Success(res)     => res
      case Failure(message) =>
        throw new FileNotFoundException(s"Could not locate perUserKeyManagerFactory keyStore file: $message")
    }

    val passwordChars = password.map(_.toCharArray).getOrElse(Array.emptyCharArray)
    val keyStore = Using.resource(resource.inputStream) { is =>
      val ks = KeyStore.getInstance(KeyStoreType)
      ks.load(is, passwordChars)
      ks
    }

    // sorting so all the load generators of a cluster compute the very same order and hence get disjoint shards
    val aliases = keyStore.aliases.asScala.filter(keyStore.isKeyEntry).toArray.sorted
    require(aliases.nonEmpty, s"perUserKeyManagerFactory keyStore $path doesn't contain any key entry")

    val protection = new KeyStore.PasswordProtection(passwordChars)
    val keyManagerFactories = aliases.map(newKeyManagerFactory(keyStore, _, passwordChars, protection))
    val count = keyManagerFactories.length
    logger.info(s"perUserKeyManagerFactory keyStore $path contains ${aliases.length} key entries, this load generator will use $count of them")

    // userIds are 1 based and unique load generator wide
    userId => {
      val index = userId - 1
      if (index >= count) {
        throw new ExhaustedKeyStoreException(
          s"perUserKeyManagerFactory keyStore $path only provides $count key entries to this load generator, can't assign one to virtual user #$userId"
        )
      }
      keyManagerFactories(index.toInt)
    }
  }

  private def newKeyManagerFactory(
      keyStore: KeyStore,
      alias: String,
      passwordChars: Array[Char],
      protection: KeyStore.PasswordProtection
  ): KeyManagerFactory = {
    val singleEntryKeyStore = KeyStore.getInstance(KeyStoreType)
    singleEntryKeyStore.load(null, passwordChars)
    singleEntryKeyStore.setEntry(alias, keyStore.getEntry(alias, protection), protection)
    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    kmf.init(singleEntryKeyStore, passwordChars)
    kmf
  }
}
