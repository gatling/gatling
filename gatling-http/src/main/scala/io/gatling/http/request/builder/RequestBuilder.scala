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

package io.gatling.http.request.builder

import io.gatling.commons.validation._
import io.gatling.core.check.Validator
import io.gatling.core.session._
import io.gatling.core.session.el.El
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.status.{ HttpStatusCheckBuilder, HttpStatusCheckMaterializer }
import io.gatling.http.client.Request
import io.gatling.http.client.oauth.{ ConsumerKey, OAuthSignatureCalculator, RequestToken }
import io.gatling.http.client.realm.Realm
import io.gatling.http.client.uri.Uri
import io.gatling.http.protocol.Proxy
import io.gatling.http.util.HttpHelper
import io.gatling.internal.quicklens._

import io.netty.handler.codec.http.HttpMethod

object CommonAttributes {
  def apply(requestName: Expression[String], method: Either[Expression[String], HttpMethod], urlOrURI: Either[Expression[String], Uri]): CommonAttributes =
    new CommonAttributes(
      requestName,
      method,
      urlOrURI,
      disableUrlEncoding = None,
      queryParams = Nil,
      headers = Map.empty,
      realm = None,
      proxy = None,
      signatureCalculator = None,
      ignoreProtocolHeaders = false
    )
}

final case class CommonAttributes(
    requestName: Expression[String],
    method: Either[Expression[String], HttpMethod],
    urlOrURI: Either[Expression[String], Uri],
    disableUrlEncoding: Option[Boolean],
    queryParams: List[HttpParam],
    headers: Map[CharSequence, Expression[String]],
    realm: Option[Expression[Realm]],
    proxy: Option[Proxy],
    signatureCalculator: Option[(Request, Session) => Validation[Request]],
    ignoreProtocolHeaders: Boolean
)

object RequestBuilder {

  /**
   * This is the default HTTP check used to verify that the response status is 2XX
   */
  val DefaultHttpCheck: HttpCheck = {
    val okStatusValidator: Validator[Int] = new Validator[Int] {
      override val name: String = "in([200, 209], 304)"
      override def apply(actual: Option[Int], displayActualValue: Boolean): Validation[Option[Int]] = actual match {
        case Some(actualValue) =>
          if (HttpHelper.isOk(actualValue))
            actual.success
          else
            s"found $actualValue".failure
        case _ => Validator.FoundNothingFailure
      }
    }

    HttpStatusCheckBuilder.find.validate(okStatusValidator.expressionSuccess).build(HttpStatusCheckMaterializer.Instance)
  }

  val AcceptAllHeaderValueExpression: Expression[String] = "*/*".expressionSuccess
  val AcceptCssHeaderValueExpression: Expression[String] = "text/css,*/*;q=0.1".expressionSuccess

  private[http] def oauth1SignatureCalculator(
      consumerKey: Expression[String],
      clientSharedSecret: Expression[String],
      token: Expression[String],
      tokenSecret: Expression[String],
      useAuthorizationHeader: Boolean
  ): (Request, Session) => Validation[Request] =
    (request, session) =>
      for {
        ck <- consumerKey(session)
        css <- clientSharedSecret(session)
        tk <- token(session)
        tks <- tokenSecret(session)
      } yield new OAuthSignatureCalculator(new ConsumerKey(ck, css), new RequestToken(tk, tks), useAuthorizationHeader).apply(request)
}

abstract class RequestBuilder[B <: RequestBuilder[B]] {
  protected def commonAttributes: CommonAttributes

  protected def newInstance(commonAttributes: CommonAttributes): B

  def queryParam(name: Expression[String], value: Expression[Any]): B = queryParam(SimpleParam(name, value))
  def multivaluedQueryParam(name: Expression[String], values: Expression[Seq[Any]]): B = queryParam(MultivaluedParam(name, values))

  def queryParamSeq(seq: Seq[(String, Any)]): B = queryParamSeq(tupleSeq2SeqExpression(seq))
  def queryParamSeq(seq: Expression[Seq[(String, Any)]]): B = queryParam(ParamSeq(seq))

  def queryParamMap(map: Map[String, Any]): B = queryParamSeq(tupleSeq2SeqExpression(map.toSeq))
  def queryParamMap(map: Expression[Map[String, Any]]): B = queryParam(ParamMap(map))

  private def queryParam(param: HttpParam): B = newInstance(modify(commonAttributes)(_.queryParams)(_ ::: List(param)))

  /**
   * Adds a header to the request
   *
   * @param name
   *   the name of the header
   * @param value
   *   the value of the header
   */
  def header(name: CharSequence, value: Expression[String]): B = newInstance(modify(commonAttributes)(_.headers)(_ + (name -> value)))

  /**
   * Adds several headers to the request at the same time
   *
   * @param newHeaders
   *   a scala map containing the headers to add
   */
  def headers(newHeaders: Map[_ <: CharSequence, String]): B =
    newInstance(modify(commonAttributes)(_.headers)(_ ++ newHeaders.view.mapValues(_.el[String])))

  def ignoreProtocolHeaders: B = newInstance(modify(commonAttributes)(_.ignoreProtocolHeaders).setTo(true))

  /**
   * Adds BASIC authentication to the request
   *
   * @param username
   *   the username needed
   * @param password
   *   the password needed
   */
  def basicAuth(username: Expression[String], password: Expression[String]): B = authRealm(HttpHelper.buildBasicAuthRealm(username, password))
  def digestAuth(username: Expression[String], password: Expression[String]): B = authRealm(HttpHelper.buildDigestAuthRealm(username, password))
  private def authRealm(realm: Expression[Realm]): B = newInstance(modify(commonAttributes)(_.realm).setTo(Some(realm)))

  def disableUrlEncoding: B = newInstance(modify(commonAttributes)(_.disableUrlEncoding).setTo(Some(true)))

  def proxy(httpProxy: Proxy): B = newInstance(modify(commonAttributes)(_.proxy).setTo(Some(httpProxy)))

  def sign(calculator: (Request, Session) => Validation[Request]): B = newInstance(modify(commonAttributes)(_.signatureCalculator).setTo(Some(calculator)))

  def signWithOAuth1(consumerKey: Expression[String], clientSharedSecret: Expression[String], token: Expression[String], tokenSecret: Expression[String]): B =
    signWithOAuth1(consumerKey, clientSharedSecret, token, tokenSecret, useAuthoriationHeader = true)

  def signWithOAuth1(
      consumerKey: Expression[String],
      clientSharedSecret: Expression[String],
      token: Expression[String],
      tokenSecret: Expression[String],
      useAuthoriationHeader: Boolean
  ): B =
    sign(RequestBuilder.oauth1SignatureCalculator(consumerKey, clientSharedSecret, token, tokenSecret, useAuthoriationHeader))
}
