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
package io.gatling.recorder.model

import scala.concurrent.duration.{ Duration, DurationLong }
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.http.util.HttpHelper
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.util.collection.RichSeq
import scala.collection.mutable
import scala.collection.mutable.{
  Map,
  SynchronizedMap,
  HashMap
}
import scala.collection.immutable.TreeMap
import scala.collection.immutable.HashSet

/**
 * <pre>
 *
 * In order to structure the scripts in a reasonable way,
 * the scenarios are composed of "navigations" which is conceptually
 * consistent with W3C standards in the area of browsing/navigating.
 *
 * http://www.w3.org/TR/html5/browsers.html#navigate
 * http://www.w3.org/TR/navigation-timing/
 *
 * "Certain actions cause the browsing context to navigate to a new resource.
 * A user agent may provide various ways for the user to explicitly cause a
 * browsing context to navigate, in addition to those defined in this specification"
 *
 * 1) In the context of a web application a navigation could map to a "Page" which leads to
 * using the page object pattern.
 *
 * http://martinfowler.com/bliki/PageObject.html
 * https://code.google.com/p/selenium/wiki/PageObjects
 *
 * 2) In the context of Apps (Android/iOS) the navigation would map to a screen and screen object
 *
 * http://rubygemtsl.com/2014/01/06/designing-maintainable-calabash-tests-using-screen-objects-2/
 *
 * </pre>
 */
class NavigationModel {

  val requestList = new mutable.ArrayBuffer[(Long, ExecModel)] with mutable.SynchronizedBuffer[(Long, ExecModel)]
  var name: String = "_undefined_"

  def +=(a: (Long, ExecModel)) = {
    requestList += a
  }

}
