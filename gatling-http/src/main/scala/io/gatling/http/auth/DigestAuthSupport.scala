/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import scala.jdk.CollectionConverters._

import io.gatling.commons.validation._
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.client.realm.DigestRealm
import io.gatling.http.client.uri.Uri

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.{ HttpHeaders, HttpMethod }
import io.netty.handler.codec.http.HttpHeaderNames.WWW_AUTHENTICATE

private[http] object DigestAuthSupport extends StrictLogging {

  val DigestAuthChallengesAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.auth.digest"

  private def getStore(session: Session): Option[DigestAuthChallengeStore] =
    session.attributes.get(DigestAuthChallengesAttributeName).map(_.asInstanceOf[DigestAuthChallengeStore])

  def getAuthorization(session: Session, requestMethod: HttpMethod, requestUri: Uri, username: String, password: String): Option[String] =
    for {
      store <- getStore(session)
      authorization <- store.getAuthorization(requestMethod, requestUri, username, password)
    } yield authorization

  private def getOrCreateDigestAuthChallenges(session: Session) =
    getStore(session) match {
      case Some(digestAuthChallenges) => digestAuthChallenges
      case _                          => DigestAuthChallengeStore.Empty
    }

  def storeChallenges(session: Session, challenges: Seq[DigestAuth.Challenge]): Session = {
    val store = getOrCreateDigestAuthChallenges(session)
    session.set(DigestAuthChallengesAttributeName, store.storeChallenges(challenges))
  }

  def extractChallenges(headers: HttpHeaders, requestUri: Uri): Seq[DigestAuth.Challenge] = {
    val authenticateHeaders = headers.getAll(WWW_AUTHENTICATE)
    if (authenticateHeaders == null || authenticateHeaders.isEmpty) {
      Nil
    } else {
      authenticateHeaders.asScala.toSeq
        .collect {
          case header if header.startsWith("Digest ") =>
            DigestAuth.parseWwwAuthenticateHeader(header, requestUri)
        }
        .flatMap {
          case Success(challenge) =>
            challenge :: Nil
          case Failure(error) =>
            logger.info(s"Failed to parse Digest header: $error")
            Nil
        }
    }
  }

  def realmWithAuthorizationGen(session: Session, digestRealm: DigestRealm): DigestRealm =
    digestRealm.withAuthorizationGen(new DigestRealm.AuthorizationGen() {
      override def apply(requestMethod: HttpMethod, requestUri: Uri, username: String, password: String): String =
        DigestAuthSupport.getAuthorization(session, requestMethod, requestUri, username, password).orNull
    })
}
