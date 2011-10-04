/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.context.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context
import akka.actor.Uuid
import com.ning.http.client.Cookie
import scala.collection.mutable.HashMap

/**
 * Class used only as validation for ContextBuilder
 */
abstract class TRUE

/**
 * Companion object of class ContextBuilder
 */
object ContextBuilder {
  /**
   * This method is available only if the context builder contains a scenario name,
   * a user id and the write actor uuid
   *
   * @param builder the builder that complies with the contract
   * @return a context with all required information
   */
  implicit def enableBuild(builder: ContextBuilder[TRUE, TRUE, TRUE]) = new {
    def build(): Context = {
      val context = new Context(builder.scenarioName.get, builder.userId.get, builder.writeActorUuid.get, builder.cookies.get, builder.data.get)
      builder.logger.debug("Built Context")
      context
    }
  }

  /**
   * Creates an initialized ContextBuilder
   */
  def newContext = new ContextBuilder(None, None, None, Some(List()), Some(Map()))
}

/**
 * This class allows a safe creation of contexts via the builder pattern
 *
 * @constructor creates a new ContextBuilder
 * @param scenarioName the name of the current scenario
 * @param userId the id of the current user
 * @param writeActorUuid the uuid of the actor responsible for logging
 * @param cookies the cookies that were received and will be sent
 * @param data the values stored in the context
 */
class ContextBuilder[HSN, HUID, HWAU](val scenarioName: Option[String], val userId: Option[Int], val writeActorUuid: Option[Uuid], val cookies: Option[List[Cookie]], val data: Option[Map[String, String]])
    extends Logging {

  /**
   * Add all values from another context to this builder
   *
   * @param context the context from which values are copied
   * @return a new builder with the same values as the context
   */
  def fromContext(context: Context) = new ContextBuilder[TRUE, TRUE, TRUE](Some(context.getScenarioName), Some(context.getUserId), Some(context.getWriteActorUuid), Some(context.getCookies), Some(context.getData))

  /**
   * Adds userId to builder
   *
   * @param userId the current user id
   * @return a new builder with userId set
   */
  def withUserId(userId: Int) = new ContextBuilder[HSN, TRUE, HWAU](scenarioName, Some(userId), writeActorUuid, cookies, data)

  /**
   * Adds userId to builder
   *
   * @param userId the current user id
   * @return a new builder with userId set
   */
  def withWriteActorUuid(writeActorUuid: Uuid) = new ContextBuilder[HSN, HUID, TRUE](scenarioName, userId, Some(writeActorUuid), cookies, data)

  /**
   * Adds userId to builder
   *
   * @param userId the current user id
   * @return a new builder with userId set
   */
  def withScenarioName(scenarioName: String) = new ContextBuilder[TRUE, HUID, HWAU](Some(scenarioName), userId, writeActorUuid, cookies, data)

  /**
   * Adds data to builder
   *
   * @param data the data to be stored in the context
   * @return a new builder with data set
   */
  def withData(data: Map[String, String]) = new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, cookies, Some(data))

  /**
   * Adds an attribute to builder
   *
   * @param attr the attribute to be added in the context ("key", "value")
   * @return a new builder with attribute set
   */
  def setAttribute(attr: Tuple2[String, String]) = {
    logger.debug("Setting '{}'='{}'", attr._1, attr._2)
    new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, cookies, Some(data.get + (attr._1 -> attr._2)))
  }

  /**
   * Removes a value from context
   *
   * @param attrKey the key of the value to be removed
   * @return a new builder with value unset
   */
  def unsetAttribute(attrKey: String) = new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, cookies, Some(data.get - attrKey))

  /**
   * Gets the value of an attribute
   *
   * @param attrKey the key of the requested attribute
   * @return the value requested as an option
   */
  def getAttribute(attrKey: String) = data.get.get(attrKey)

  /**
   * Sets the duration of the last action
   *
   * @param value the duration of the last action
   * @return a new builder with the duration set
   */
  def setDuration(value: Long) = unsetAttribute(Context.LAST_ACTION_DURATION_ATTR_NAME) setAttribute (Context.LAST_ACTION_DURATION_ATTR_NAME, value.toString)

  /**
   * Adds cookies to builder.
   *
   * Replaces existing cookies with new values, adds new ones
   *
   * @param cookies cookies to be added to context
   * @return a new builder with cookies set
   */
  def setCookies(cookies: java.util.List[Cookie]) = {
    var cookiesMap: HashMap[String, Cookie] = new HashMap
    for (cookie <- this.cookies.getOrElse(Nil))
      cookiesMap += (cookie.getName -> cookie)

    val it = cookies.iterator
    while (it.hasNext) {
      val cookie = it.next
      cookiesMap += (cookie.getName -> cookie)
    }

    var cookiesList: List[Cookie] = Nil
    for (c <- cookiesMap)
      cookiesList = c._2 :: cookiesList

    logger.debug("Cookies put in ContextBuilder: {}", cookiesList)
    new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, Some(cookiesList), data)
  }
}
