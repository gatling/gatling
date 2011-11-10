/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.config.HttpProtocolConfigurationBuilder
import com.excilys.ebi.gatling.http.check.body.HttpBodyRegExpCheckBuilder
import com.excilys.ebi.gatling.http.check.body.HttpBodyXPathCheckBuilder
import com.excilys.ebi.gatling.http.check.header.HttpHeaderCheckBuilder
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheckBuilder

object Predef {
	def http(requestName: String) = HttpRequestActionBuilder.http(requestName)

	def httpConfig = HttpProtocolConfigurationBuilder.httpConfig
	implicit def toHttpProtocolConfiguration(builder: HttpProtocolConfigurationBuilder) = HttpProtocolConfigurationBuilder.toHttpProtocolConfiguration(builder)

	def regexpEquals(what: Context => String, occurrence: Int, expected: String) = HttpBodyRegExpCheckBuilder.regexpEquals(what, occurrence, expected)
	def regexpEquals(what: Context => String, expected: String) = HttpBodyRegExpCheckBuilder.regexpEquals(what, expected)
	def regexpEquals(expression: String, occurrence: Int, expected: String) = HttpBodyRegExpCheckBuilder.regexpEquals(expression, occurrence, expected)
	def regexpEquals(expression: String, expected: String) = HttpBodyRegExpCheckBuilder.regexpEquals(expression, expected)
	def regexpNotEquals(what: Context => String, occurrence: Int, expected: String) = HttpBodyRegExpCheckBuilder.regexpNotEquals(what, occurrence, expected)
	def regexpNotEquals(what: Context => String, expected: String) = HttpBodyRegExpCheckBuilder.regexpNotEquals(what, expected)
	def regexpNotEquals(expression: String, occurrence: Int, expected: String) = HttpBodyRegExpCheckBuilder.regexpNotEquals(expression, occurrence, expected)
	def regexpNotEquals(expression: String, expected: String) = HttpBodyRegExpCheckBuilder.regexpNotEquals(expression, expected)
	def regexpExists(what: Context => String, occurrence: Int) = HttpBodyRegExpCheckBuilder.regexpExists(what, occurrence)
	def regexpExists(what: Context => String) = HttpBodyRegExpCheckBuilder.regexpExists(what)
	def regexpExists(expression: String, occurrence: Int) = HttpBodyRegExpCheckBuilder.regexpExists(expression, occurrence)
	def regexpExists(expression: String) = HttpBodyRegExpCheckBuilder.regexpExists(expression)
	def regexpNotExists(what: Context => String, occurrence: Int) = HttpBodyRegExpCheckBuilder.regexpNotExists(what, occurrence)
	def regexpNotExists(what: Context => String) = HttpBodyRegExpCheckBuilder.regexpNotExists(what)
	def regexpNotExists(expression: String, occurrence: Int) = HttpBodyRegExpCheckBuilder.regexpNotExists(expression, occurrence)
	def regexpNotExists(expression: String) = HttpBodyRegExpCheckBuilder.regexpNotExists(expression)
	def regexp(what: Context => String, occurrence: Int) = HttpBodyRegExpCheckBuilder.regexp(what, occurrence)
	def regexp(what: Context => String) = HttpBodyRegExpCheckBuilder.regexp(what)
	def regexp(expression: String, occurrence: Int) = HttpBodyRegExpCheckBuilder.regexp(expression, occurrence)
	def regexp(expression: String) = HttpBodyRegExpCheckBuilder.regexp(expression)

	def xpathEquals(what: Context => String, occurrence: Int, expected: String) = HttpBodyXPathCheckBuilder.xpathEquals(what, occurrence, expected)
	def xpathEquals(what: Context => String, expected: String) = HttpBodyXPathCheckBuilder.xpathEquals(what, expected)
	def xpathEquals(expression: String, occurrence: Int, expected: String) = HttpBodyXPathCheckBuilder.xpathEquals(expression, occurrence, expected)
	def xpathEquals(expression: String, expected: String) = HttpBodyXPathCheckBuilder.xpathEquals(expression, expected)
	def xpathNotEquals(what: Context => String, occurrence: Int, expected: String) = HttpBodyXPathCheckBuilder.xpathNotEquals(what, occurrence, expected)
	def xpathNotEquals(what: Context => String, expected: String) = HttpBodyXPathCheckBuilder.xpathNotEquals(what, expected)
	def xpathNotEquals(expression: String, occurrence: Int, expected: String) = HttpBodyXPathCheckBuilder.xpathNotEquals(expression, occurrence, expected)
	def xpathNotEquals(expression: String, expected: String) = HttpBodyXPathCheckBuilder.xpathNotEquals(expression, expected)
	def xpathExists(what: Context => String, occurrence: Int) = HttpBodyXPathCheckBuilder.xpathExists(what, occurrence)
	def xpathExists(what: Context => String) = HttpBodyXPathCheckBuilder.xpathExists(what)
	def xpathExists(expression: String, occurrence: Int) = HttpBodyXPathCheckBuilder.xpathExists(expression, occurrence)
	def xpathExists(expression: String) = HttpBodyXPathCheckBuilder.xpathExists(expression)
	def xpathNotExists(what: Context => String, occurrence: Int) = HttpBodyXPathCheckBuilder.xpathNotExists(what, occurrence)
	def xpathNotExists(what: Context => String) = HttpBodyXPathCheckBuilder.xpathNotExists(what)
	def xpathNotExists(expression: String, occurrence: Int) = HttpBodyXPathCheckBuilder.xpathNotExists(expression, occurrence)
	def xpathNotExists(expression: String) = HttpBodyXPathCheckBuilder.xpathNotExists(expression)
	def xpath(what: Context => String, occurrence: Int) = HttpBodyXPathCheckBuilder.xpath(what, occurrence)
	def xpath(what: Context => String) = HttpBodyXPathCheckBuilder.xpath(what)
	def xpath(expression: String, occurrence: Int) = HttpBodyXPathCheckBuilder.xpath(expression, occurrence)
	def xpath(expression: String) = HttpBodyXPathCheckBuilder.xpath(expression)

	def headerEquals(what: Context => String, expected: String) = HttpHeaderCheckBuilder.headerEquals(what, expected)
	def headerEquals(headerName: String, expected: String) = HttpHeaderCheckBuilder.headerEquals(headerName, expected)
	def headerNotEquals(what: Context => String, expected: String) = HttpHeaderCheckBuilder.headerNotEquals(what, expected)
	def headerNotEquals(headerName: String, expected: String) = HttpHeaderCheckBuilder.headerNotEquals(headerName, expected)
	def headerExists(what: Context => String) = HttpHeaderCheckBuilder.headerExists(what)
	def headerExists(headerName: String) = HttpHeaderCheckBuilder.headerExists(headerName)
	def headerNotExists(what: Context => String) = HttpHeaderCheckBuilder.headerNotExists(what)
	def headerNotExists(headerName: String) = HttpHeaderCheckBuilder.headerNotExists(headerName)
	def header(what: Context => String) = HttpHeaderCheckBuilder.header(what)
	def header(headerName: String) = HttpHeaderCheckBuilder.header(headerName)

	def statusInRange(range: Range) = HttpStatusCheckBuilder.statusInRange(range)
	def status(status: Int) = HttpStatusCheckBuilder.status(status)

	val ACCEPT = "Accept";
	val ACCEPT_CHARSET = "Accept-Charset";
	val ACCEPT_ENCODING = "Accept-Encoding";
	val ACCEPT_LANGUAGE = "Accept-Language";
	val ACCEPT_RANGES = "Accept-Ranges";
	val ACCEPT_PATCH = "Accept-Patch";
	val AGE = "Age";
	val ALLOW = "Allow";
	val AUTHORIZATION = "Authorization";
	val CACHE_CONTROL = "Cache-Control";
	val CONNECTION = "Connection";
	val CONTENT_BASE = "Content-Base";
	val CONTENT_ENCODING = "Content-Encoding";
	val CONTENT_LANGUAGE = "Content-Language";
	val CONTENT_LENGTH = "Content-Length";
	val CONTENT_LOCATION = "Content-Location";
	val CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
	val CONTENT_MD5 = "Content-MD5";
	val CONTENT_RANGE = "Content-Range";
	val CONTENT_TYPE = "Content-Type";
	val COOKIE = "Cookie";
	val DATE = "Date";
	val ETAG = "ETag";
	val EXPECT = "Expect";
	val EXPIRES = "Expires";
	val FROM = "From";
	val HOST = "Host";
	val IF_MATCH = "If-Match";
	val IF_MODIFIED_SINCE = "If-Modified-Since";
	val IF_NONE_MATCH = "If-None-Match";
	val IF_RANGE = "If-Range";
	val IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	val LAST_MODIFIED = "Last-Modified";
	val LOCATION = "Location";
	val MAX_FORWARDS = "Max-Forwards";
	val ORIGIN = "Origin";
	val PRAGMA = "Pragma";
	val PROXY_AUTHENTICATE = "Proxy-Authenticate";
	val PROXY_AUTHORIZATION = "Proxy-Authorization";
	val RANGE = "Range";
	val REFERER = "Referer";
	val RETRY_AFTER = "Retry-After";
	val SEC_WEBSOCKET_KEY1 = "Sec-WebSocket-Key1";
	val SEC_WEBSOCKET_KEY2 = "Sec-WebSocket-Key2";
	val SEC_WEBSOCKET_LOCATION = "Sec-WebSocket-Location";
	val SEC_WEBSOCKET_ORIGIN = "Sec-WebSocket-Origin";
	val SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
	val SERVER = "Server";
	val SET_COOKIE = "Set-Cookie";
	val SET_COOKIE2 = "Set-Cookie2";
	val TE = "TE";
	val TRAILER = "Trailer";
	val TRANSFER_ENCODING = "Transfer-Encoding";
	val UPGRADE = "Upgrade";
	val USER_AGENT = "User-Agent";
	val VARY = "Vary";
	val VIA = "Via";
	val WARNING = "Warning";
	val WEBSOCKET_LOCATION = "WebSocket-Location";
	val WEBSOCKET_ORIGIN = "WebSocket-Origin";
	val WEBSOCKET_PROTOCOL = "WebSocket-Protocol";
	val WWW_AUTHENTICATE = "WWW-Authenticate";
}