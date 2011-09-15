package com.excilys.ebi.gatling.core.provider.capture

import com.ximpleware.VTDGen
import com.ximpleware.VTDNav
import com.ximpleware.AutoPilot
import com.excilys.ebi.gatling.core.log.Logging

object XPathCaptureProvider extends Logging {

  var captureProviderPool: Map[String, XPathCaptureProvider] = Map.empty

  def prepare(identifier: String, xmlContent: Array[Byte]) = {
    captureProviderPool += (identifier -> new XPathCaptureProvider(xmlContent))
  }

  def capture(identifier: String, expression: Any) = {
    logger.debug("[XPathCaptureProvider] Capturing with expression : {}", expression)
    captureProviderPool.get(identifier).get.capture(expression)
  }

  def clear(identifier: String) = {
    captureProviderPool -= identifier
  }

}
private[capture] class XPathCaptureProvider(xmlContent: Array[Byte]) extends AbstractCaptureProvider {

  var vtdEngine: VTDGen = null
  var vn: VTDNav = null
  var ap: AutoPilot = null

  vtdEngine = new VTDGen
  vtdEngine.setDoc(xmlContent)
  vtdEngine.parse(false)
  vn = vtdEngine.getNav()
  ap = new AutoPilot(vn)

  def capture(expression: Any): Option[String] = {
    ap.selectXPath(expression.toString)
    val value = Some(ap.evalXPathToString)
    logger.debug("XPATH CAPTURE: {}", value)
    value
  }

}