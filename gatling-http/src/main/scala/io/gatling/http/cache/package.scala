/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.http

import java.util.{ Collection => JCollection }

import scala.collection.breakOut
import scala.collection.JavaConverters._

import io.netty.handler.codec.http.cookie.Cookie

package object cache {

  type Cookies = Map[String, String]

  object Cookies {
    def apply(cookies: JCollection[Cookie]): Cookies =
      cookies.asScala.map(cookie => cookie.name -> cookie.path)(breakOut)
  }
}
