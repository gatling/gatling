/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.recorder.http.handler

import java.net.{ InetSocketAddress, URI }

import scala.collection.JavaConversions.asScalaBuffer

import org.jboss.netty.channel.{ Channel, ChannelFuture, ChannelHandlerContext, ExceptionEvent }
import org.jboss.netty.handler.codec.http.{ DefaultHttpRequest, DefaultHttpResponse, HttpMethod, HttpRequest, HttpResponseStatus, HttpVersion }
import org.jboss.netty.handler.ssl.SslHandler

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory
import io.gatling.recorder.http.handler.ChannelFutures.function2ChannelFutureListener
import io.gatling.recorder.http.ssl.SSLEngineFactory
import javax.net.ssl.SSLException

class BrowserHttpsRequestHandler(proxy: HttpProxy) extends AbstractBrowserRequestHandler(proxy.controller) with StrictLogging {

	private var _channel: Option[Channel] = None
	var targetHostURI: URI = _

	def propagateRequest(ctx: ChannelHandlerContext, request: HttpRequest) {

		def handleConnect {
			targetHostURI = new URI("https://" + request.getUri)
			ctx.getChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
			ctx.getPipeline.addFirst(BootstrapFactory.SSL_HANDLER_NAME, new SslHandler(SSLEngineFactory.newServerSSLEngine))
		}

		def buildConnectRequest = {
			val connect = new DefaultHttpRequest(request.getProtocolVersion, HttpMethod.CONNECT, s"${targetHostURI.getHost}:${targetHostURI.getPort}")
			for (header <- request.headers.entries) connect.headers.add(header.getKey, header.getValue)
			connect
		}

		def handlePropagatableRequest {

			// set full uri so that it's correctly recorded FIXME ugly
			request.setUri(targetHostURI.resolve(request.getUri).toString)

			_clientChannel match {
				case Some(channel) if channel.isConnected =>
					channel.write(AbstractBrowserRequestHandler.buildRequestWithRelativeURI(request))

				case _ =>
					_clientChannel = None
					(configuration.proxy.outgoing.host, configuration.proxy.outgoing.port) match {
						case (Some(proxyHost), Some(proxyPort)) =>
							// proxy: have to CONNECT over HTTP, before performing request over HTTPS
							proxy.clientBootstrap
								.connect(new InetSocketAddress(proxyHost, proxyPort))
								.addListener { connectFuture: ChannelFuture =>
									connectFuture.getChannel.getPipeline.addLast(BootstrapFactory.GATLING_HANDLER_NAME, new ServerHttpResponseHandler(proxy.controller, ctx, request, true))
									_clientChannel = Some(connectFuture.getChannel)
									connectFuture.getChannel.write(buildConnectRequest)
								}

						case _ =>
							// direct connection
							proxy.secureClientBootstrap
								.connect(new InetSocketAddress(targetHostURI.getHost, targetHostURI.getPort))
								.addListener { connectFuture: ChannelFuture =>
									connectFuture.getChannel.getPipeline.get(BootstrapFactory.SSL_HANDLER_NAME).asInstanceOf[SslHandler].handshake
										.addListener { handshakeFuture: ChannelFuture =>
											handshakeFuture.getChannel.getPipeline.addLast(BootstrapFactory.GATLING_HANDLER_NAME, new ServerHttpResponseHandler(proxy.controller, ctx, request, false))
											_clientChannel = Some(connectFuture.getChannel)
											handshakeFuture.getChannel.write(AbstractBrowserRequestHandler.buildRequestWithRelativeURI(request))
										}
								}
					}
			}
		}

		logger.info(s"Received ${request.getMethod} on ${request.getUri}")
		if (request.getMethod == HttpMethod.CONNECT)
			handleConnect
		else
			handlePropagatableRequest
	}

	override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {

		e.getCause match {
			case ssle: SSLException =>
				logger.error(s"SSLException ${ssle.getMessage}, did you accept the certificate for $targetHostURI?")
				proxy.controller.secureConnection(targetHostURI)
				ctx.sendUpstream(e)
				ctx.getChannel.close
				_clientChannel.map(_.close)
			case _ => super.exceptionCaught(ctx, e)
		}
	}
}
