/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.http

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.config.HttpProtocolConfigurationBuilder
import com.excilys.ebi.gatling.http.check.body.HttpBodyRegexCheckBuilder
import com.excilys.ebi.gatling.http.check.body.HttpBodyXPathCheckBuilder
import com.excilys.ebi.gatling.http.check.header.HttpHeaderCheckBuilder
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheckBuilder
import com.excilys.ebi.gatling.core.check.CheckBuilder
import com.excilys.ebi.gatling.http.config.HttpProxyBuilder

object Predef {
	def http(requestName: String) = HttpRequestActionBuilder.http(requestName)

	def httpConfig = HttpProtocolConfigurationBuilder.httpConfig
	implicit def toHttpProtocolConfiguration(hpb: HttpProxyBuilder) = HttpProxyBuilder.toHttpProtocolConfiguration(hpb)
	implicit def toHttpProtocolConfiguration(builder: HttpProtocolConfigurationBuilder) = HttpProtocolConfigurationBuilder.toHttpProtocolConfiguration(builder)

	implicit def intToString(i: Int) = CheckBuilder.intToString(i)

	def regex(what: Session => String) = HttpBodyRegexCheckBuilder.regex(what)
	def regex(expression: String) = HttpBodyRegexCheckBuilder.regex(expression)

	def xpath(what: Session => String) = HttpBodyXPathCheckBuilder.xpath(what)
	def xpath(expression: String) = HttpBodyXPathCheckBuilder.xpath(expression)

	def header(what: Session => String) = HttpHeaderCheckBuilder.header(what)
	def header(headerName: String) = HttpHeaderCheckBuilder.header(headerName)

	def status = HttpStatusCheckBuilder.status

	/* MIME types */
	val APPLICATION_JAVASCRIPT = "application/javascript"
	val APPLICATION_JSON = "application/json"
	val APPLICATION_OCTET_STREAM = "application/octet-stream"
	val APPLICATION_PDF = "application/pdf"
	val APPLICATION_ZIP = "application/zip"
	val APPLICATION_GZIP = "application/x-gzip"
	val APPLICATION_XML = "application/xml"
	val AUDIO_MP4 = "audio/mp4"
	val AUDIO_MPEG = "audio/mpeg"
	val AUDIO_OGG = "audio/ogg"
	val AUDIO_VORBIS = "audio/vorbis"
	val AUDIO_WEBM = "audio/webm"
	val IMAGE_PNG = "image/png"
	val IMAGE_JPEG = "image/jpeg"
	val IMAGE_GIF = "image/gif"
	val IMAGE_SVG = "image/svg+xml"
	val MULTIPART_FORM_DATA = "multipart/form-data"
	val TEXT_CSS = "text/css"
	val TEXT_CSV = "text/csv"
	val TEXT_HTML = "text/html"
	val TEXT_JAVASCRIPT = "text/javascript"
	val TEXT_PLAIN = "text/plain"
	val TEXT_XML = "text/xml"
	val VIDEO_MPEG = "video/mpeg"
	val VIDEO_MP4 = "video/mp4"
	val VIDEO_OGG = "video/ogg"
	val VIDEO_WEBM = "video/webm"
	val VIDEO_QUICKTIME = "video/quicktime"

	/* Headers */
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