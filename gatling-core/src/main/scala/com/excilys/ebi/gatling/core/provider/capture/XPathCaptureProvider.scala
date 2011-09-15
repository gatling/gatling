package com.excilys.ebi.gatling.core.provider.capture

import com.ximpleware.CustomVTDGen
import com.ximpleware.VTDNav
import com.ximpleware.AutoPilot

class XPathCaptureProvider(xmlContent: Array[Byte]) extends AbstractCaptureProvider {

  val vtdEngine = new CustomVTDGen
  vtdEngine.setDoc(xmlContent)
  vtdEngine.parse(false)

  var vn = vtdEngine.getNav()
  var ap = new AutoPilot(vn)

  def capture(expression: Any): Option[String] = {
    logger.debug("[XPathCaptureProvider] Capturing with expression : {}", expression)
    ap.selectXPath(expression.toString)
    val value = Some(ap.evalXPathToString)
    logger.debug("XPATH CAPTURE: {}", value)
    value
  }

}