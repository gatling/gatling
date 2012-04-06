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
import org.jboss.netty.handler.codec.http.HttpHeaders

import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.http.check.body.{ HttpBodyXPathCheckBuilder, HttpBodyRegexCheckBuilder, HttpBodyJsonPathCheckBuilder }
import com.excilys.ebi.gatling.http.check.header.HttpHeaderCheckBuilder
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheckBuilder
import com.excilys.ebi.gatling.http.config.{ HttpProxyBuilder, HttpProtocolConfigurationBuilder }
import com.excilys.ebi.gatling.http.request.builder.HttpRequestBaseBuilder

object Predef {
	
	def http(requestName: String) = HttpRequestBaseBuilder.http(requestName)

	def httpConfig = HttpProtocolConfigurationBuilder.httpConfig
	implicit def toHttpProtocolConfiguration(hpb: HttpProxyBuilder) = HttpProxyBuilder.toHttpProtocolConfiguration(hpb)
	implicit def toHttpProtocolConfiguration(builder: HttpProtocolConfigurationBuilder) = HttpProtocolConfigurationBuilder.toHttpProtocolConfiguration(builder)

	def regex(expression: EvaluatableString) = HttpBodyRegexCheckBuilder.regex(expression)
	def xpath(expression: EvaluatableString, namespaces: List[(String, String)] = Nil) = HttpBodyXPathCheckBuilder.xpath(expression, namespaces)
	def jsonPath(expression: EvaluatableString) = HttpBodyJsonPathCheckBuilder.jsonPath(expression)
	def header(expression: EvaluatableString) = HttpHeaderCheckBuilder.header(expression)
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
	val ACCEPT = HttpHeaders.Names.ACCEPT
	val ACCEPT_CHARSET = HttpHeaders.Names.ACCEPT_CHARSET
	val ACCEPT_ENCODING = HttpHeaders.Names.ACCEPT_ENCODING
	val ACCEPT_LANGUAGE = HttpHeaders.Names.ACCEPT_LANGUAGE
	val ACCEPT_RANGES = HttpHeaders.Names.ACCEPT_RANGES
	val ACCEPT_PATCH = HttpHeaders.Names.ACCEPT_PATCH
	val AGE = HttpHeaders.Names.AGE
	val ALLOW = HttpHeaders.Names.ALLOW
	val AUTHORIZATION = HttpHeaders.Names.AUTHORIZATION
	val CACHE_CONTROL = HttpHeaders.Names.CACHE_CONTROL
	val CONNECTION = HttpHeaders.Names.CONNECTION
	val CONTENT_BASE = HttpHeaders.Names.CONTENT_BASE
	val CONTENT_ENCODING = HttpHeaders.Names.CONTENT_ENCODING
	val CONTENT_LANGUAGE = HttpHeaders.Names.CONTENT_LANGUAGE
	val CONTENT_LENGTH = HttpHeaders.Names.CONTENT_LENGTH
	val CONTENT_LOCATION = HttpHeaders.Names.CONTENT_LOCATION
	val CONTENT_TRANSFER_ENCODING = HttpHeaders.Names.CONTENT_TRANSFER_ENCODING
	val CONTENT_MD5 = HttpHeaders.Names.CONTENT_MD5
	val CONTENT_RANGE = HttpHeaders.Names.CONTENT_RANGE
	val CONTENT_TYPE = HttpHeaders.Names.CONTENT_TYPE
	val COOKIE = HttpHeaders.Names.COOKIE
	val DATE = HttpHeaders.Names.DATE
	val ETAG = HttpHeaders.Names.ETAG
	val EXPECT = HttpHeaders.Names.EXPECT
	val EXPIRES = HttpHeaders.Names.EXPIRES
	val FROM = HttpHeaders.Names.FROM
	val HOST = HttpHeaders.Names.HOST
	val IF_MATCH = HttpHeaders.Names.IF_MATCH
	val IF_MODIFIED_SINCE = HttpHeaders.Names.IF_MODIFIED_SINCE
	val IF_NONE_MATCH = HttpHeaders.Names.IF_NONE_MATCH
	val IF_RANGE = HttpHeaders.Names.IF_RANGE
	val IF_UNMODIFIED_SINCE = HttpHeaders.Names.IF_UNMODIFIED_SINCE
	val LAST_MODIFIED = HttpHeaders.Names.LAST_MODIFIED
	val LOCATION = HttpHeaders.Names.LOCATION
	val MAX_FORWARDS = HttpHeaders.Names.MAX_FORWARDS
	val ORIGIN = HttpHeaders.Names.ORIGIN
	val PRAGMA = HttpHeaders.Names.PRAGMA
	val PROXY_AUTHENTICATE = HttpHeaders.Names.PROXY_AUTHENTICATE
	val PROXY_AUTHORIZATION = HttpHeaders.Names.PROXY_AUTHORIZATION
	val RANGE = HttpHeaders.Names.RANGE
	val REFERER = HttpHeaders.Names.RANGE
	val RETRY_AFTER = HttpHeaders.Names.RETRY_AFTER
	val SEC_WEBSOCKET_KEY1 = HttpHeaders.Names.SEC_WEBSOCKET_KEY1
	val SEC_WEBSOCKET_KEY2 = HttpHeaders.Names.SEC_WEBSOCKET_KEY2
	val SEC_WEBSOCKET_LOCATION = HttpHeaders.Names.SEC_WEBSOCKET_LOCATION
	val SEC_WEBSOCKET_ORIGIN = HttpHeaders.Names.SEC_WEBSOCKET_ORIGIN
	val SEC_WEBSOCKET_PROTOCOL = HttpHeaders.Names.SEC_WEBSOCKET_PROTOCOL
	val SERVER = HttpHeaders.Names.SERVER
	val SET_COOKIE = HttpHeaders.Names.SET_COOKIE
	val SET_COOKIE2 = HttpHeaders.Names.SET_COOKIE2
	val TE = HttpHeaders.Names.TE
	val TRAILER = HttpHeaders.Names.TRAILER
	val TRANSFER_ENCODING = HttpHeaders.Names.TRANSFER_ENCODING
	val UPGRADE = HttpHeaders.Names.UPGRADE
	val USER_AGENT = HttpHeaders.Names.USER_AGENT
	val VARY = HttpHeaders.Names.VARY
	val VIA = HttpHeaders.Names.VIA
	val WARNING = HttpHeaders.Names.WARNING
	val WEBSOCKET_LOCATION = HttpHeaders.Names.WEBSOCKET_LOCATION
	val WEBSOCKET_ORIGIN = HttpHeaders.Names.WEBSOCKET_ORIGIN
	val WEBSOCKET_PROTOCOL = HttpHeaders.Names.WEBSOCKET_PROTOCOL
	val WWW_AUTHENTICATE = HttpHeaders.Names.WEBSOCKET_PROTOCOL
}