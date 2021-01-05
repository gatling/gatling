/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

// FIXME contribute upstream
private[gatling] object MissingNettyHttpHeaderNames {
  val DNT: AsciiString = AsciiString.cached("dnt")
  val UpgradeInsecureRequests: AsciiString = AsciiString.cached("upgrade-insecure-requests")
  val XRequestedWith: AsciiString = AsciiString.cached("x-requested-with")
}

object HeaderNames {
  val Accept: CharSequence = HttpHeaderNames.ACCEPT
  val AcceptCharset: CharSequence = HttpHeaderNames.ACCEPT_CHARSET
  val AcceptEncoding: CharSequence = HttpHeaderNames.ACCEPT_ENCODING
  val AcceptLanguage: CharSequence = HttpHeaderNames.ACCEPT_LANGUAGE
  val AcceptPatch: CharSequence = HttpHeaderNames.ACCEPT_PATCH
  val AcceptRanges: CharSequence = HttpHeaderNames.ACCEPT_RANGES
  val Age: CharSequence = HttpHeaderNames.AGE
  val Allow: CharSequence = HttpHeaderNames.ALLOW
  val Authorization: CharSequence = HttpHeaderNames.AUTHORIZATION
  val CacheControl: CharSequence = HttpHeaderNames.CACHE_CONTROL
  val Connection: CharSequence = HttpHeaderNames.CONNECTION
  val ContentBase: CharSequence = HttpHeaderNames.CONTENT_BASE
  val ContentEncoding: CharSequence = HttpHeaderNames.CONTENT_ENCODING
  val ContentLanguage: CharSequence = HttpHeaderNames.CONTENT_LANGUAGE
  val ContentLength: CharSequence = HttpHeaderNames.CONTENT_LENGTH
  val ContentLocation: CharSequence = HttpHeaderNames.CONTENT_LOCATION
  val ContentTransferEncoding: CharSequence = HttpHeaderNames.CONTENT_TRANSFER_ENCODING
  val ContentMD5: CharSequence = HttpHeaderNames.CONTENT_MD5
  val ContentRange: CharSequence = HttpHeaderNames.CONTENT_RANGE
  val ContentType: CharSequence = HttpHeaderNames.CONTENT_TYPE
  val Cookie: CharSequence = HttpHeaderNames.COOKIE
  val Date: CharSequence = HttpHeaderNames.DATE
  val DNT: CharSequence = MissingNettyHttpHeaderNames.DNT
  val ETag: CharSequence = HttpHeaderNames.ETAG
  val Expect: CharSequence = HttpHeaderNames.EXPECT
  val Expires: CharSequence = HttpHeaderNames.EXPIRES
  val From: CharSequence = HttpHeaderNames.FROM
  val Host: CharSequence = HttpHeaderNames.HOST
  val IfMatch: CharSequence = HttpHeaderNames.IF_MATCH
  val IfModifiedSince: CharSequence = HttpHeaderNames.IF_MODIFIED_SINCE
  val IfNoneMatch: CharSequence = HttpHeaderNames.IF_NONE_MATCH
  val IfRange: CharSequence = HttpHeaderNames.IF_RANGE
  val IfUnmodifiedSince: CharSequence = HttpHeaderNames.IF_UNMODIFIED_SINCE
  val LastModified: CharSequence = HttpHeaderNames.LAST_MODIFIED
  val Location: CharSequence = HttpHeaderNames.LOCATION
  val MaxForwards: CharSequence = HttpHeaderNames.MAX_FORWARDS
  val Origin: CharSequence = HttpHeaderNames.ORIGIN
  val Pragma: CharSequence = HttpHeaderNames.PRAGMA
  val ProxyAuthenticate: CharSequence = HttpHeaderNames.PROXY_AUTHENTICATE
  val ProxyAuthorization: CharSequence = HttpHeaderNames.PROXY_AUTHORIZATION
  val Range: CharSequence = HttpHeaderNames.RANGE
  val Referer: CharSequence = HttpHeaderNames.REFERER
  val RetryAfter: CharSequence = HttpHeaderNames.RETRY_AFTER
  val SecWebSocketKey1: CharSequence = HttpHeaderNames.SEC_WEBSOCKET_KEY1
  val SecWebSocketKey2: CharSequence = HttpHeaderNames.SEC_WEBSOCKET_KEY1
  val SecWebSocketLocation: CharSequence = HttpHeaderNames.SEC_WEBSOCKET_LOCATION
  val SecWebSocketOrigin: CharSequence = HttpHeaderNames.SEC_WEBSOCKET_ORIGIN
  val SecWebSocketProtocol: CharSequence = HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL
  val SecWebSocketVersion: CharSequence = HttpHeaderNames.SEC_WEBSOCKET_VERSION
  val SecWebSocketKey: CharSequence = HttpHeaderNames.SEC_WEBSOCKET_KEY
  val SecWebSocketAccept: CharSequence = HttpHeaderNames.SEC_WEBSOCKET_ACCEPT
  val SecWebSocketExtensions: CharSequence = HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS
  val Server: CharSequence = HttpHeaderNames.SERVER
  val SetCookie: CharSequence = HttpHeaderNames.SET_COOKIE
  val SetCookie2: CharSequence = HttpHeaderNames.SET_COOKIE2
  val TE: CharSequence = HttpHeaderNames.TE
  val Trailer: CharSequence = HttpHeaderNames.TRAILER
  val TransferEncoding: CharSequence = HttpHeaderNames.TRANSFER_ENCODING
  val Upgrade: CharSequence = HttpHeaderNames.UPGRADE
  val UserAgent: CharSequence = HttpHeaderNames.USER_AGENT
  val UpgradeInsecureRequests: CharSequence = MissingNettyHttpHeaderNames.UpgradeInsecureRequests
  val Vary: CharSequence = HttpHeaderNames.VARY
  val Via: CharSequence = HttpHeaderNames.VIA
  val Warning: CharSequence = HttpHeaderNames.WARNING
  val WebSocketLocation: CharSequence = HttpHeaderNames.WEBSOCKET_LOCATION
  val WebSocketOrigin: CharSequence = HttpHeaderNames.WEBSOCKET_ORIGIN
  val WebSocketProtocol: CharSequence = HttpHeaderNames.WEBSOCKET_PROTOCOL
  val WWWAuthenticate: CharSequence = HttpHeaderNames.WWW_AUTHENTICATE
  val XRequestedWith: CharSequence = MissingNettyHttpHeaderNames.XRequestedWith
}

// FIXME contribute upstream
private[gatling] object MissingNettyHttpHeaderValues {
  val ApplicationXml: AsciiString = AsciiString.cached("application/xml")
  val ApplicationXhtml: AsciiString = AsciiString.cached("application/xhtml+xml")
  val TextCss: AsciiString = AsciiString.cached("text/css")
  val TextHtml: AsciiString = AsciiString.cached("text/html")
  val TextEventStream: AsciiString = AsciiString.cached("text/event-stream")
  val XmlHttpRequest: AsciiString = AsciiString.cached("XMLHttpRequest")
}

object HeaderValues {
  val ApplicationJson: String = HttpHeaderValues.APPLICATION_JSON.toString
  val ApplicationOctetStream: String = HttpHeaderValues.APPLICATION_OCTET_STREAM.toString
  val ApplicationFormUrlEncoded: String = HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString
  val ApplicationXml: String = MissingNettyHttpHeaderValues.ApplicationXml.toString
  val ApplicationXhtml: String = MissingNettyHttpHeaderValues.ApplicationXhtml.toString
  val Close: String = HttpHeaderValues.CLOSE.toString
  val MultipartFormData: String = HttpHeaderValues.MULTIPART_FORM_DATA.toString
  val NoCache: String = HttpHeaderValues.NO_CACHE.toString
  val NoStore: String = HttpHeaderValues.NO_STORE.toString
  val TextCss: String = MissingNettyHttpHeaderValues.TextCss.toString
  val TextHtml: String = MissingNettyHttpHeaderValues.TextHtml.toString
  val TextPlain: String = HttpHeaderValues.TEXT_PLAIN.toString
  val TextEventStream: String = MissingNettyHttpHeaderValues.TextEventStream.toString
  val XmlHttpRequest: String = MissingNettyHttpHeaderValues.XmlHttpRequest.toString
}
