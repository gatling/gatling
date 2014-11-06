package io.gatling.http.request.builder.sse

import com.ning.http.client.Request
import io.gatling.core.session.Expression
import io.gatling.http.action.sse.SseGetActionBuilder
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.{ RequestBuilder, CommonAttributes }

/**
 * @author ctranxuan
 */
object SseGetRequestBuilder {

  implicit def toActionBuilder(requestBuilder: SseGetRequestBuilder) = new SseGetActionBuilder(requestBuilder.commonAttributes.requestName, requestBuilder.sseName, requestBuilder)
}

class SseGetRequestBuilder(commonAttributes: CommonAttributes, val sseName: String) extends RequestBuilder[SseGetRequestBuilder](commonAttributes) {

  override private[http] def newInstance(commonAttributes: CommonAttributes): SseGetRequestBuilder = new SseGetRequestBuilder(commonAttributes, sseName)

  def build(protocol: HttpProtocol): Expression[Request] = new SseRequestExpressionBuilder(commonAttributes, protocol).build
}
