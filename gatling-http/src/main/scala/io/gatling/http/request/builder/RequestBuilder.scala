/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request.builder

import java.net.InetAddress

import com.ning.http.client._
import com.ning.http.client.uri.Uri
import com.typesafe.scalalogging.StrictLogging

import io.gatling.core.session._
import io.gatling.core.session.el.EL
import io.gatling.core.validation._
import io.gatling.http.check.status.HttpStatusCheckBuilder._
import io.gatling.http.util.HttpHelper._
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.ahc.ProxyConverter
import io.gatling.http.config.Proxy
import io.gatling.http.util.HttpHelper

import scala.annotation.tailrec

case class CommonAttributes(
  requestName: Expression[String],
  method: String,
  urlOrURI: Either[Expression[String], Uri],
  disableUrlEncoding: Option[Boolean] = None,
  queryParams: List[HttpParam] = Nil,
  headers: Map[String, Expression[String]] = Map.empty,
  realm: Option[Expression[Realm]] = None,
  virtualHost: Option[Expression[String]] = None,
  address: Option[InetAddress] = None,
  proxies: Option[(ProxyServer, ProxyServer)] = None,
  signatureCalculator: Option[Expression[SignatureCalculator]] = None)

object RequestBuilder {

  /**
   * This is the default HTTP check used to verify that the response status is 2XX
   */
  val OkCodesExpression = OkCodes.expression

  val DefaultHttpCheck = Status.find.in(OkCodesExpression).build

  val JsonHeaderValueExpression = HeaderValues.ApplicationJson.expression
  val XmlHeaderValueExpression = HeaderValues.ApplicationXml.expression
  val AllHeaderHeaderValueExpression = "*/*".expression
  val CssHeaderHeaderValueExpression = "text/css,*/*;q=0.1".expression
}

abstract class RequestBuilder[B <: RequestBuilder[B]](val commonAttributes: CommonAttributes) extends StrictLogging {

  private[http] def newInstance(commonAttributes: CommonAttributes): B

  def queryParam(key: Expression[String], value: Expression[Any]): B = queryParam(SimpleParam(key, value))
  def multivaluedQueryParam(key: Expression[String], values: Expression[Seq[Any]]): B = queryParam(MultivaluedParam(key, values))

  private[http] def resolveIterable(iterable: Iterable[(String, Expression[Any])]): Expression[Seq[(String, Any)]] = {

      @tailrec
      def resolveRec(session: Session, entries: Iterator[(String, Expression[Any])], acc: List[(String, Any)]): Validation[Seq[(String, Any)]] = {
        if (entries.isEmpty)
          acc.reverse.success
        else {
          val (key, elValue) = entries.next()
          elValue(session) match {
            case Success(value)   => resolveRec(session, entries, (key -> value) :: acc)
            case failure: Failure => failure
          }
        }
      }

    (session: Session) => resolveRec(session, iterable.iterator, Nil)
  }

  private[http] def seq2SeqExpression(seq: Seq[(String, Any)]): Expression[Seq[(String, Any)]] = {
    val elValues: Seq[(String, Expression[Any])] = seq.map {
      case (key, value) =>
        val elValue = value match {
          case s: String => s.el
          case v         => v.expression
        }
        key -> elValue
    }

    resolveIterable(elValues)
  }

  private[http] def map2SeqExpression(map: Map[String, Any]): Expression[Seq[(String, Any)]] = {
    val elValues: Map[String, Expression[Any]] = map.mapValues {
      case s: String => s.el
      case v         => v.expression
    }

    resolveIterable(elValues)
  }

  def queryParamSeq(seq: Seq[(String, Any)]): B = queryParamSeq(seq2SeqExpression(seq))
  def queryParamSeq(seq: Expression[Seq[(String, Any)]]): B = queryParam(ParamSeq(seq))

  def queryParamMap(map: Map[String, Any]): B = queryParamSeq(map2SeqExpression(map))
  def queryParamMap(map: Expression[Map[String, Any]]): B = queryParam(ParamMap(map))

  private def queryParam(param: HttpParam): B = newInstance(commonAttributes.copy(queryParams = param :: commonAttributes.queryParams))

  /**
   * Adds a header to the request
   *
   * @param name the name of the header
   * @param value the value of the header
   */
  def header(name: String, value: Expression[String]): B = newInstance(commonAttributes.copy(headers = commonAttributes.headers + (name -> value)))

  /**
   * Adds several headers to the request at the same time
   *
   * @param newHeaders a scala map containing the headers to add
   */
  def headers(newHeaders: Map[String, String]): B = newInstance(commonAttributes.copy(headers = commonAttributes.headers ++ newHeaders.mapValues(_.el[String])))

  /**
   * Adds Accept and Content-Type headers to the request set with "application/json" values
   */
  def asJSON: B = header(HeaderNames.Accept, RequestBuilder.JsonHeaderValueExpression).header(HeaderNames.ContentType, RequestBuilder.JsonHeaderValueExpression)

  /**
   * Adds Accept and Content-Type headers to the request set with "application/xml" values
   */
  def asXML: B = header(HeaderNames.Accept, RequestBuilder.XmlHeaderValueExpression).header(HeaderNames.ContentType, RequestBuilder.XmlHeaderValueExpression)

  /**
   * Adds BASIC authentication to the request
   *
   * @param username the username needed
   * @param password the password needed
   */
  def basicAuth(username: Expression[String], password: Expression[String]): B = authRealm(HttpHelper.buildBasicAuthRealm(username, password))
  def digestAuth(username: Expression[String], password: Expression[String]): B = authRealm(HttpHelper.buildDigestAuthRealm(username, password))
  def ntlmAuth(username: Expression[String], password: Expression[String], ntlmDomain: Expression[String], ntlmHost: Expression[String]): B =
    authRealm(HttpHelper.buildNTLMAuthRealm(username, password, ntlmDomain, ntlmHost))
  def authRealm(realm: Expression[Realm]): B = newInstance(commonAttributes.copy(realm = Some(realm)))

  /**
   * @param virtualHost a virtual host to override default compute one
   */
  def virtualHost(virtualHost: Expression[String]): B = newInstance(commonAttributes.copy(virtualHost = Some(virtualHost)))

  def address(address: InetAddress): B = newInstance(commonAttributes.copy(address = Some(address)))

  def disableUrlEncoding: B = newInstance(commonAttributes.copy(disableUrlEncoding = Some(true)))

  def proxy(httpProxy: Proxy): B = newInstance(commonAttributes.copy(proxies = Some(httpProxy.proxyServers)))

  def signatureCalculator(calculator: Expression[SignatureCalculator]): B = newInstance(commonAttributes.copy(signatureCalculator = Some(calculator)))
  def signatureCalculator(calculator: SignatureCalculator): B = signatureCalculator(calculator.expression)
  def signatureCalculator(calculator: (Request, RequestBuilderBase[_]) => Unit): B = signatureCalculator(new SignatureCalculator {
    def calculateAndAddSignature(request: Request, requestBuilder: RequestBuilderBase[_]): Unit = calculator(request, requestBuilder)
  })
}
