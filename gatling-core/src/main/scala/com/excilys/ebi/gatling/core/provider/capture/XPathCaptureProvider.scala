package com.excilys.ebi.gatling.core.provider.capture

import com.ximpleware.VTDGen
import com.ximpleware.VTDNav
import com.ximpleware.AutoPilot

class XPathCaptureProvider(xmlContent: Array[Byte]) extends AbstractCaptureProvider {

  var vtdEngine: VTDGen = null
  var vn: VTDNav = null
  var ap: AutoPilot = null

  vtdEngine = new VTDGen
  vtdEngine.setDoc(xmlContent)
  vtdEngine.parse(false)
  vn = vtdEngine.getNav()
  ap = new AutoPilot(vn)

  def capture(expression: Any): Option[String] = {
    logger.debug("[XPathCaptureProvider] Capturing with expression : {}", expression)
    ap.selectXPath(expression.toString)
    val value = Some(ap.evalXPathToString)
    logger.debug("XPATH CAPTURE: {}", value)
    value
  }

}