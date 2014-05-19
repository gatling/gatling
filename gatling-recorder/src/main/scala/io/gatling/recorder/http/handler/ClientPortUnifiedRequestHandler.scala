package io.gatling.recorder.http.handler

import java.net.URI

import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.channel.Channels

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.recorder.http.channel.BootstrapFactory
import io.gatling.recorder.http.HttpProxy



class ClientPortUnifiedRequestHandler(proxy: HttpProxy, pipeline: ChannelPipeline) extends SimpleChannelHandler with StrictLogging {

  var done = false

  override def messageReceived(requestContext: ChannelHandlerContext, event: MessageEvent) {

    try {
      if (!done) {

        event.getMessage match {
          case request: HttpRequest => {

            val uri = new URI(request.getUri())
            uri.getScheme() match {

              case "http" => setProtocolHandler(false, pipeline)
              
              case https => {
                request.getMethod().toString() match {
                  
                  case "CONNECT" => setProtocolHandler(true, pipeline)
                  case unknown => logger.warn("Received unknown scheme (http|https): $unknown in " + request)
                }
              }
            }
          }
          case unknown => logger.warn("Received unknown message: $unknown , in event : " + event)
        }
      }

    } finally {
      Channels.fireMessageReceived(requestContext, event.getMessage())
      done = true
    }
  }

  def setProtocolHandler(ssl: Boolean, pipeline: ChannelPipeline) {

    if (ssl)
      BootstrapFactory.setGatlingProtocolHandler(pipeline, new ClientHttpsRequestHandler(proxy))
    else
      BootstrapFactory.setGatlingProtocolHandler(pipeline, new ClientHttpRequestHandler(proxy))
  }
}