package io.gatling.recorder.http.handler

import java.net.{ InetSocketAddress, URI }
import org.jboss.netty.channel.{ Channel, ChannelFuture, ChannelHandlerContext }
import org.jboss.netty.handler.codec.http.{ DefaultHttpResponse, HttpRequest, HttpResponseStatus, HttpVersion }
import io.gatling.recorder.http.HttpProxy
import io.gatling.core.result.message.MessageEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.buffer.EmptyChannelBuffer
import org.jboss.netty.handler.codec.http.HttpMethod

class ClientPortUnifiedRequestHandler(proxy: HttpProxy) extends ClientRequestHandler(proxy) {

  val sslHandler = new ClientHttpsRequestHandler(proxy)
  val nonSslHandler = new ClientHttpRequestHandler(proxy)

  def propagateRequest(requestContext: ChannelHandlerContext, request: HttpRequest) {

    request.getMethod match {
      case HttpMethod.CONNECT => sslHandler.propagateRequest(requestContext, request)
      case _ => {
        val uri = new URI(request.getUri())
        uri.getPort match {
          case -1 => uri.getScheme match {
            case "https" | "wss" => sslHandler.propagateRequest(requestContext, request)
            case "http"          => nonSslHandler.propagateRequest(requestContext, request)
            case _               => sslHandler.propagateRequest(requestContext, request)    // ssl requests come through with the pathinfo only here
          }
          case 80  => nonSslHandler.propagateRequest(requestContext, request)
          case 443 => sslHandler.propagateRequest(requestContext, request)
          
          case _ => nonSslHandler.propagateRequest(requestContext, request)
        }

      }

    }
  }
}