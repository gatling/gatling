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
package io.gatling.jms.request

import io.gatling.core.session.Expression
import io.gatling.jms.JmsCheck

/**
 * JmsAttributes carries around the JMS settings.
 * <p>
 * As the JmsReqReplyBuilder is building a request from the DSL, it uses this object
 * to represent the in progress request. Once the request is built it can then be used
 * so that the JmsReqReplyAction knows exactly what message to send.
 *
 * @author jasonk@bluedevel.com
 */
case class JmsAttributes(
  requestName:       String,
  destination:       JmsDestination,
  selector:          Option[String],
  message:           JmsMessage,
  messageProperties: Map[Expression[String], Expression[Any]] = Map.empty,
  jmsType:           Option[Expression[String]]               = None,
  checks:            List[JmsCheck]                           = Nil
)
