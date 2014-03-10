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

import java.io.IOException
import java.net.{ InetSocketAddress, URI }

import scala.collection.JavaConversions.asScalaBuffer

import org.jboss.netty.channel.{ ChannelFuture, ChannelHandlerContext, ExceptionEvent }
import org.jboss.netty.handler.codec.http.{ DefaultHttpRequest, DefaultHttpResponse, HttpMethod, HttpRequest, HttpResponseStatus, HttpVersion }
import org.jboss.netty.handler.ssl.SslHandler

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory
import io.gatling.recorder.http.handler.ChannelFutures.function2ChannelFutureListener
import io.gatling.recorder.http.ssl.SSLEngineFactory
import javax.net.ssl.SSLException

class ClientHttpsRequestHandler(proxy: HttpProxy) extends ClientRequestHandler(proxy) with StrictLogging {

	var targetHostURI: URI = _

	def propagateRequest(requestContext: ChannelHandlerContext, request: HttpRequest) {

		def handleConnect() {
			targetHostURI = new URI("https://" + request.getUri)
			requestContext.getChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
			requestContext.getPipeline.addFirst(BootstrapFactory.SSL_HANDLER_NAME, new SslHandler(SSLEngineFactory.newServerSSLEngine))
		}

		def buildConnectRequest = {
			val connect = new DefaultHttpRequest(request.getProtocolVersion, HttpMethod.CONNECT, s"${targetHostURI.getHost}:${targetHostURI.getPort}")
			for (header <- request.headers.entries) connect.headers.add(header.getKey, header.getValue)
			connect
		}

		def handlePropagatableRequest() {

			// set full uri so that it's correctly recorded FIXME ugly
			request.setUri(targetHostURI.resolve(request.getUri).toString)

			_serverChannel match {
				case Some(serverChannel) if serverChannel.isConnected && serverChannel.isOpen =>
					serverChannel.getPipeline.get(classOf[ServerHttpResponseHandler]).request = request
					serverChannel.write(ClientRequestHandler.buildRequestWithRelativeURI(request))

				case _ =>
					_serverChannel = None
					(proxy.outgoingHost, proxy.outgoingPort) match {
						case (Some(proxyHost), Some(proxyPort)) =>
							// proxy: have to CONNECT over HTTP, before performing request over HTTPS
							proxy.clientBootstrap
								.connect(new InetSocketAddress(proxyHost, proxyPort))
								.addListener { connectFuture: ChannelFuture =>
									val serverChannel = connectFuture.getChannel
									serverChannel.getPipeline.addLast(BootstrapFactory.GATLING_HANDLER_NAME, new ServerHttpResponseHandler(proxy.controller, requestContext.getChannel, request, true))
									_serverChannel = Some(serverChannel)
									serverChannel.write(buildConnectRequest)
								}

						case _ =>
							// direct connection
							proxy.secureClientBootstrap
								.connect(new InetSocketAddress(targetHostURI.getHost, targetHostURI.getPort))
								.addListener { connectFuture: ChannelFuture =>
									connectFuture.getChannel.getPipeline.get(BootstrapFactory.SSL_HANDLER_NAME).asInstanceOf[SslHandler].handshake
										.addListener { handshakeFuture: ChannelFuture =>
											val serverChannel = handshakeFuture.getChannel
											serverChannel.getPipeline.addLast(BootstrapFactory.GATLING_HANDLER_NAME, new ServerHttpResponseHandler(proxy.controller, requestContext.getChannel, request, false))
											_serverChannel = Some(serverChannel)
											serverChannel.write(ClientRequestHandler.buildRequestWithRelativeURI(request))
										}
								}
					}
			}
		}

		logger.info(s"Received ${request.getMethod} on ${request.getUri}")
		request.getMethod match {
			case HttpMethod.CONNECT => handleConnect
			case _ => handlePropagatableRequest
		}
	}

	override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {

		def handleSslException(e: Exception) {
			logger.error(s"${e.getClass.getSimpleName} ${e.getMessage}, did you accept the certificate for $targetHostURI?")
			proxy.controller.secureConnection(targetHostURI)
			ctx.getChannel.close
			_serverChannel.map(_.close)
		}

		e.getCause match {
			case ioe: IOException if (ioe.getMessage == "Broken pipe") => handleSslException(ioe)
			case ssle: SSLException => handleSslException(ssle)
			case _ => super.exceptionCaught(ctx, e)
		}
	}
}
