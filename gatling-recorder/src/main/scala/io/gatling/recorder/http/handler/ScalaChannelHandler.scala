/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.recorder.http.handler

import io.netty.channel.{ Channel, ChannelFutureListener, ChannelFuture }
import io.netty.util.concurrent.{ GenericFutureListener, Future }

private[handler] trait ScalaChannelHandler {

  implicit def function2ChannelFutureListener(f: ChannelFuture => Any) = new ChannelFutureListener {
    override def operationComplete(future: ChannelFuture): Unit = f(future)
  }

  implicit def function2GenericFutureListener(f: Future[Channel] => Any) = new GenericFutureListener[Future[Channel]] {
    override def operationComplete(future: Future[Channel]): Unit = f(future)
  }
}
