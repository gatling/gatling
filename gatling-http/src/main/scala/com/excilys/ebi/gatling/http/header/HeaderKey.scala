package com.excilys.ebi.gatling.http.header

class HeaderKey

case class AcceptRangesHeader() extends HeaderKey
case class AgeHeader() extends HeaderKey { override def toString = "Age" }
case class AllowHeader() extends HeaderKey { override def toString = "Allow" }
case class CacheControlHeader() extends HeaderKey { override def toString = "Cache-Control" }
case class ConnectionHeader() extends HeaderKey { override def toString = "Connection" }
case class ContentEncodingHeader() extends HeaderKey { override def toString = "Content-Encoding" }
case class ContentLanguageHeader() extends HeaderKey { override def toString = "Content-Language" }
case class ContentLengthHeader() extends HeaderKey { override def toString = "Content-Length" }
case class ContentLocationHeader() extends HeaderKey { override def toString = "Content-Location" }
case class ContentMD5Header() extends HeaderKey { override def toString = "Content-MD5" }
case class ContentDispositionHeader() extends HeaderKey { override def toString = "Content-Disposition" }
case class ContentRangeHeader() extends HeaderKey { override def toString = "Content-Range" }
case class ContentTypeHeader() extends HeaderKey { override def toString = "Content-Type" }
case class DateHeader() extends HeaderKey { override def toString = "Date" }
case class ETagHeader() extends HeaderKey { override def toString = "ETag" }
case class ExpiresHeader() extends HeaderKey { override def toString = "Expires" }
case class LastModifiedHeader() extends HeaderKey { override def toString = "Last-Modified" }
case class LinkHeader() extends HeaderKey { override def toString = "Link" }
case class LocationHeader() extends HeaderKey { override def toString = "Location" }
case class P3PHeader() extends HeaderKey { override def toString = "P3P" }
case class PragmaHeader() extends HeaderKey { override def toString = "Pragma" }
case class ProxyAuthenticateHeader() extends HeaderKey { override def toString = "Proxy-Authenticate" }
case class RefreshHeader() extends HeaderKey { override def toString = "Refresh" }
case class RetryAfterHeader() extends HeaderKey { override def toString = "Retry-After" }
case class ServerHeader() extends HeaderKey { override def toString = "Server" }
case class SetCookieHeader() extends HeaderKey { override def toString = "Set-Cookie" }
case class StrictTransportSecurityHeader() extends HeaderKey { override def toString = "Strict-Transport-Security" }
case class TrailerHeader() extends HeaderKey { override def toString = "Trailer" }
case class TransferEncodingHeader() extends HeaderKey { override def toString = "Transfer-Encoding" }
case class VaryHeader() extends HeaderKey { override def toString = "Vary" }
case class ViaHeader() extends HeaderKey { override def toString = "Via" }
case class WarningHeader() extends HeaderKey { override def toString = "Warning" }
case class WWWAuthenticateHeader() extends HeaderKey { override def toString = "WWW-Authenticate" }