/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.http.ahc

import java.lang.System.nanoTime

import com.ning.http.client.Response

sealed trait HttpEvent

case class OnHeaderWriteCompleted(time: Long = nanoTime) extends HttpEvent
case class OnContentWriteCompleted(time: Long = nanoTime) extends HttpEvent
case class OnStatusReceived(time: Long = nanoTime) extends HttpEvent
case class OnHeadersReceived extends HttpEvent
case class OnBodyPartReceived extends HttpEvent
case class OnCompleted(response: Response, time: Long = nanoTime) extends HttpEvent
case class OnThrowable(errorMessage: String, time: Long = nanoTime) extends HttpEvent