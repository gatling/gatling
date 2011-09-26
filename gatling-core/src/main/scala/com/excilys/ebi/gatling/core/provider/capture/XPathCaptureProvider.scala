package com.excilys.ebi.gatling.core.provider.capture

import com.ximpleware.CustomVTDGen
import com.ximpleware.VTDNav
import com.ximpleware.AutoPilot
import org.apache.commons.lang3.StringUtils

class XPathCaptureProvider(xmlContent: Array[Byte]) extends AbstractCaptureProvider {

  val vtdEngine = new CustomVTDGen
  vtdEngine.setDoc(xmlContent)
  vtdEngine.parse(false)

  var vn = vtdEngine.getNav()
  var ap = new AutoPilot(vn)

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