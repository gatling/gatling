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
package com.excilys.ebi.gatling.core.provider.capture

import com.ximpleware.CustomVTDGen
import com.ximpleware.VTDNav
import com.ximpleware.AutoPilot
import org.apache.commons.lang3.StringUtils

/**
 * This class is a built-in provider that helps searching with XPath Expressions
 *
 * it requires a well formatted XML document, otherwise, it will throw an exception
 *
 * @constructor creates a new XPathCaptureProvider
 * @param xmlContent the XML document as bytes in which the XPath search will be applied
 */
class XPathCaptureProvider(xmlContent: Array[Byte]) extends AbstractCaptureProvider {

  val vtdEngine = new CustomVTDGen
  vtdEngine.setDoc(xmlContent)
  vtdEngine.parse(false)

  var vn = vtdEngine.getNav()
  var ap = new AutoPilot(vn)

  /**
   * The actual capture happens here. The XPath expression is searched for and the first
   * result is returned if existing.
   *
   * @param expression a String containing the XPath expression to be searched
   * @return an option containing the value if found, None otherwise
   */
  def capture(expression: Any): Option[String] = {
    logger.debug("[XPathCaptureProvider] Capturing with expression : {}", expression)
    ap.selectXPath(expression.toString)
    val result = ap.evalXPathToString
    val value = if (result.equals(StringUtils.EMPTY))
      None
    else
      Some(ap.evalXPathToString)
    logger.debug("XPATH CAPTURE: {}", value)
    value
  }
}