/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.jms.check

import java.nio.charset.Charset

import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.check.{ CheckMaterializer, Preparer }
import io.gatling.jms.JmsCheck

import javax.jms.Message

class JmsBodySubstringCheckMaterializer(charset: Charset) extends CheckMaterializer[SubstringCheckType, JmsCheck, Message, String](identity) {
  override protected def preparer: Preparer[Message, String] = JmsMessageBodyPreparers.jmsStringBodyPreparer(charset)
}
