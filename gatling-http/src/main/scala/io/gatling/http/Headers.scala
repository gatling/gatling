/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.http

import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaderValues }
import io.netty.util.AsciiString

object MissingNettyHttpHeaderNames {
  val DNT: AsciiString = AsciiString.cached("dnt") // FIXME contribute upstream
  val UpgradeInsecureRequests: AsciiString = AsciiString.cached("upgrade-insecure-requests") // FIXME contribute upstream
  val XRequestedWith: AsciiString = AsciiString.cached("x-requested-with") // FIXME contribute upstream
}

@deprecated("Use io.netty.handler.codec.http.HttpHeaderNames instead. Will be removed in 3.5.0", since = "3.4.0")
object HeaderNames {
  val Accept: AsciiString = HttpHeaderNames.ACCEPT
  val AcceptCharset: AsciiString = HttpHeaderNames.ACCEPT_CHARSET
  val AcceptEncoding: AsciiString = HttpHeaderNames.ACCEPT_ENCODING
  val AcceptLanguage: AsciiString = HttpHeaderNames.ACCEPT_LANGUAGE
  val AcceptPatch: AsciiString = HttpHeaderNames.ACCEPT_PATCH
  val AcceptRanges: AsciiString = HttpHeaderNames.ACCEPT_RANGES
  val Age: AsciiString = HttpHeaderNames.AGE
  val Allow: AsciiString = HttpHeaderNames.ALLOW
  val Authorization: AsciiString = HttpHeaderNames.AUTHORIZATION
  val CacheControl: AsciiString = HttpHeaderNames.CACHE_CONTROL
  val Connection: AsciiString = HttpHeaderNames.CONNECTION
  val ContentBase: AsciiString = HttpHeaderNames.CONTENT_BASE
  val ContentEncoding: AsciiString = HttpHeaderNames.CONTENT_ENCODING
  val ContentLanguage: AsciiString = HttpHeaderNames.CONTENT_LANGUAGE
  val ContentLength: AsciiString = HttpHeaderNames.CONTENT_LENGTH
  val ContentLocation: AsciiString = HttpHeaderNames.CONTENT_LOCATION
  val ContentTransferEncoding: AsciiString = HttpHeaderNames.CONTENT_TRANSFER_ENCODING
  val ContentMD5: AsciiString = HttpHeaderNames.CONTENT_MD5
  val ContentRange: AsciiString = HttpHeaderNames.CONTENT_RANGE
  val ContentType: AsciiString = HttpHeaderNames.CONTENT_TYPE
  val Cookie: AsciiString = HttpHeaderNames.COOKIE
  val Date: AsciiString = HttpHeaderNames.DATE
  val DNT: AsciiString = MissingNettyHttpHeaderNames.DNT
  val ETag: AsciiString = HttpHeaderNames.ETAG
  val Expect: AsciiString = HttpHeaderNames.EXPECT
  val Expires: AsciiString = HttpHeaderNames.EXPIRES
  val From: AsciiString = HttpHeaderNames.FROM
  val Host: AsciiString = HttpHeaderNames.HOST
  val IfMatch: AsciiString = HttpHeaderNames.IF_MATCH
  val IfModifiedSince: AsciiString = HttpHeaderNames.IF_MODIFIED_SINCE
  val IfNoneMatch: AsciiString = HttpHeaderNames.IF_NONE_MATCH
  val IfRange: AsciiString = HttpHeaderNames.IF_RANGE
  val IfUnmodifiedSince: AsciiString = HttpHeaderNames.IF_UNMODIFIED_SINCE
  val LastModified: AsciiString = HttpHeaderNames.LAST_MODIFIED
  val Location: AsciiString = HttpHeaderNames.LOCATION
  val MaxForwards: AsciiString = HttpHeaderNames.MAX_FORWARDS
  val Origin: AsciiString = HttpHeaderNames.ORIGIN
  val Pragma: AsciiString = HttpHeaderNames.PRAGMA
  val ProxyAuthenticate: AsciiString = HttpHeaderNames.PROXY_AUTHENTICATE
  val ProxyAuthorization: AsciiString = HttpHeaderNames.PROXY_AUTHORIZATION
  val Range: AsciiString = HttpHeaderNames.RANGE
  val Referer: AsciiString = HttpHeaderNames.REFERER
  val RetryAfter: AsciiString = HttpHeaderNames.RETRY_AFTER
  val SecWebSocketKey1: AsciiString = HttpHeaderNames.SEC_WEBSOCKET_KEY1
  val SecWebSocketKey2: AsciiString = HttpHeaderNames.SEC_WEBSOCKET_KEY1
  val SecWebSocketLocation: AsciiString = HttpHeaderNames.SEC_WEBSOCKET_LOCATION
  val SecWebSocketOrigin: AsciiString = HttpHeaderNames.SEC_WEBSOCKET_ORIGIN
  val SecWebSocketProtocol: AsciiString = HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL
  val SecWebSocketVersion: AsciiString = HttpHeaderNames.SEC_WEBSOCKET_VERSION
  val SecWebSocketKey: AsciiString = HttpHeaderNames.SEC_WEBSOCKET_KEY
  val SecWebSocketAccept: AsciiString = HttpHeaderNames.SEC_WEBSOCKET_ACCEPT
  val Server: AsciiString = HttpHeaderNames.SERVER
  val SetCookie: AsciiString = HttpHeaderNames.SET_COOKIE
  val SetCookie2: AsciiString = HttpHeaderNames.SET_COOKIE2
  val TE: AsciiString = HttpHeaderNames.TE
  val Trailer: AsciiString = HttpHeaderNames.TRAILER
  val TransferEncoding: AsciiString = HttpHeaderNames.TRANSFER_ENCODING
  val Upgrade: AsciiString = HttpHeaderNames.UPGRADE
  val UserAgent: AsciiString = HttpHeaderNames.USER_AGENT
  val UpgradeInsecureRequests: AsciiString = MissingNettyHttpHeaderNames.UpgradeInsecureRequests
  val Vary: AsciiString = HttpHeaderNames.VARY
  val Via: AsciiString = HttpHeaderNames.VIA
  val Warning: AsciiString = HttpHeaderNames.WARNING
  val WebSocketLocation: AsciiString = HttpHeaderNames.WEBSOCKET_LOCATION
  val WebSocketOrigin: AsciiString = HttpHeaderNames.WEBSOCKET_ORIGIN
  val WebSocketProtocol: AsciiString = HttpHeaderNames.WEBSOCKET_PROTOCOL
  val WWWAuthenticate: AsciiString = HttpHeaderNames.WWW_AUTHENTICATE
  val XRequestedWith: AsciiString = MissingNettyHttpHeaderNames.XRequestedWith
}

object MissingNettyHttpHeaderValues {
  val ApplicationXml: AsciiString = AsciiString.cached("application/xml")
  val ApplicationXhtml: AsciiString = AsciiString.cached("application/xhtml+xml")
  val TextCss: AsciiString = AsciiString.cached("text/css")
  val TextHtml: AsciiString = AsciiString.cached("text/html")
  val TextEventStream: AsciiString = AsciiString.cached("text/event-stream")
  val XmlHttpRequest: AsciiString = AsciiString.cached("XMLHttpRequest")
}

@deprecated("Use io.netty.handler.codec.http.HttpHeaderValues instead. Will be removed in 3.5.0", since = "3.4.0")
object HeaderValues {
  val ApplicationJson: String = HttpHeaderValues.APPLICATION_JSON.toString
  val ApplicationOctetStream: String = HttpHeaderValues.APPLICATION_OCTET_STREAM.toString
  val ApplicationFormUrlEncoded: String = HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString
  val ApplicationXml: String = MissingNettyHttpHeaderValues.ApplicationXml.toString
  val Close: String = HttpHeaderValues.CLOSE.toString
  val MultipartFormData: String = HttpHeaderValues.MULTIPART_FORM_DATA.toString
  val TextPlain: String = HttpHeaderValues.TEXT_PLAIN.toString
  val NoCache: String = HttpHeaderValues.NO_CACHE.toString
  val NoStore: String = HttpHeaderValues.NO_STORE.toString
}
