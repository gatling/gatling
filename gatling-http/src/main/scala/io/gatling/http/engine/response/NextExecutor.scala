/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.http.engine.response

import io.gatling.commons.stats.{ KO, Status }
import io.gatling.commons.util.Clock
import io.gatling.core.session.Session
import io.gatling.core.util.NameGen
import io.gatling.http.engine.tx.{ HttpTx, HttpTxExecutor, ResourceTx }
import io.gatling.http.fetch.ResourceFetcher
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper.isCss

trait NextExecutor {

  def executeNext(session: Session, status: Status, response: Response): Unit
  def executeNextOnCrash(session: Session, endTimestamp: Long): Unit
  def executeRedirect(redirectTx: HttpTx): Unit
}

class RootNextExecutor(
    tx:              HttpTx,
    clock:           Clock,
    resourceFetcher: ResourceFetcher,
    httpTxExecutor:  HttpTxExecutor
) extends NextExecutor with NameGen {

  override def executeNext(session: Session, status: Status, response: Response): Unit =
    resourceFetcher.newResourceAggregatorForFetchedPage(response, tx, status) match {
      case Some(resourceFetcherActor) => resourceFetcherActor.start()
      case _                          => tx.next ! session.increaseDrift(clock.nowMillis - response.endTimestamp)
    }

  override def executeNextOnCrash(session: Session, endTimestamp: Long): Unit =
    tx.next ! session.increaseDrift(clock.nowMillis - endTimestamp)

  override def executeRedirect(redirectTx: HttpTx): Unit =
    httpTxExecutor.execute(redirectTx)
}

class ResourceNextExecutor(
    tx:         HttpTx,
    resourceTx: ResourceTx
) extends NextExecutor {

  override def executeNext(session: Session, status: Status, response: Response): Unit =
    if (isCss(response.headers)) {
      resourceTx.aggregator.onCssResourceFetched(resourceTx.uri, status, session, tx.silent, response.status, response.lastModifiedOrEtag(tx.request.requestConfig.httpProtocol), response.body.string)
    } else {
      resourceTx.aggregator.onRegularResourceFetched(resourceTx.uri, status, session, tx.silent)
    }

  override def executeNextOnCrash(session: Session, endTimestamp: Long): Unit =
    resourceTx.aggregator.onRegularResourceFetched(resourceTx.uri, KO, session, tx.silent)

  override def executeRedirect(redirectTx: HttpTx): Unit =
    resourceTx.aggregator.onRedirect(tx, redirectTx)
}
