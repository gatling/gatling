/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.http.auth

import java.util.concurrent.atomic.AtomicInteger

import io.gatling.http.client.uri.Uri

import io.netty.handler.codec.http.HttpMethod

object DigestAuthChallengeStore {
  val Empty: DigestAuthChallengeStore = new DigestAuthChallengeStore(Map.empty)

  private final case class Key(domain: Set[DigestAuth.ProtectionSpace])
  private final case class Entry(challenge: DigestAuth.Challenge, nc: AtomicInteger)
}

final class DigestAuthChallengeStore(challenges: Map[DigestAuthChallengeStore.Key, DigestAuthChallengeStore.Entry]) {
  import DigestAuthChallengeStore._

  def storeChallenges(newChallenges: Seq[DigestAuth.Challenge]): DigestAuthChallengeStore = {
    val mostSecured =
      newChallenges
        .groupBy(challenge => DigestAuthChallengeStore.Key(challenge.domain))
        .view
        .mapValues(challenges => Entry(challenges.maxBy(_.algorithm.securityLevel), new AtomicInteger))
        .toMap

    new DigestAuthChallengeStore(challenges ++ mostSecured)
  }

  def getAuthorization(requestMethod: HttpMethod, requestUri: Uri, username: String, password: String): Option[String] =
    challenges.collectFirst {
      case (key, Entry(challenge, nc)) if key.domain.exists(_.matches(requestUri)) =>
        DigestAuth.generateAuthorization(challenge, username, password, requestMethod, requestUri, nc.incrementAndGet())
    }
}
