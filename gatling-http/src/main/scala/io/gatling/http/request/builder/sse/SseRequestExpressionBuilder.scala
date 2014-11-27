package io.gatling.http.request.builder.sse

import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.{ CommonAttributes, RequestExpressionBuilder }
import io.gatling.http.util.HttpHelper

/**
 * @author ctranxuan
 */
// fixme inheriting from HttpRequestExpressionBuilder???
class SseRequestExpressionBuilder(commonAttributes: CommonAttributes, protocol: HttpProtocol) extends RequestExpressionBuilder(commonAttributes, protocol) {

  def makeAbsolute(url: String): Validation[String] =
    if (HttpHelper.isAbsoluteHttpUrl(url))
      url.success
    else
      protocol.baseURL match {
        case Some(baseURL) => (baseURL + url).success
        case _             => s"No protocol.baseURL defined but provided url is relative : $url".failure
      }
}
