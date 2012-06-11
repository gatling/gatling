/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.ahc

import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration

import akka.testkit.{TestKit, TestActorRef}

import com.ning.http.client.Request

import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.mock.Mockito

import org.junit.runner.RunWith
import com.excilys.ebi.gatling.core.session.Session
import akka.actor.{UntypedActorFactory, Actor, Props, ActorSystem}

@RunWith(classOf[JUnitRunner])
object GatlingAsyncHandlerActorSpec extends TestKit(ActorSystem("test")) with Specification with Mockito {

  "request info extraction function" should {

    "be provided with the request" in {

//      val testActorRef = TestActorRef[GatlingAsyncHandlerActor]
//      val handlerActor = actorRef.underlyingActor
//      val handlerActor = system.actorOf(Props())

      val mockRequest = mock[Request]
      val expectedExtraInfo = List("some", "custom", "data")

      def mockExtractor(actualRequest: Request): List[String] = {
        actualRequest must beEqualTo(mockRequest)
        expectedExtraInfo
      }

      val mockProtocolConfig = mock[HttpProtocolConfiguration]
      mockProtocolConfig.extraRequestInfoExtractor returns Some(mockExtractor)

      val session = new Session("scenario", 1)

      val props:Props = Props(
        creator = new GatlingAsyncHandlerActor(session, List(), null, "request name", mockRequest, false, Some(mockProtocolConfig), null)
      )
/*
      val testActorRef = new TestActorRef(system, null, props, null, "testActorRef")

      val actorRef = system.actorOf(Props(new GatlingAsyncHandlerActor(session, List(), null, "request name", mockRequest, false,
                Some(mockProtocolConfig), null)))
      val handlerActor: GatlingAsyncHandlerActor = new GatlingAsyncHandlerActor(session, List(), null, "request name", mockRequest, false,
                      Some(mockProtocolConfig), null)


      val handlerActor:GatlingAsyncHandlerActor = testActorRef.underlyingActor
      handlerActor.extractExtraRequestInfo(Some(mockProtocolConfig), mockRequest)

      there was one(mockProtocolConfig).extraRequestInfoExtractor
*/
      todo
    }

/*
    "have its exceptions caught" in {
      failure
    }
*/

  }

/*
  doAfter(){
    system.shutdown()
    this.success
  }
*/

}
