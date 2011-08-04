package com.excilys.ebi.gatling.core.provider.capture

import com.ximpleware.VTDGen
import com.ximpleware.VTDNav
import com.ximpleware.AutoPilot

object XPathCaptureProvider {

  var providersMap: Map[String, XPathCaptureProvider] = Map.empty

  def getInstance(identifier: String): XPathCaptureProvider = {
    providersMap.get(identifier).map { provider =>
      provider
    }.getOrElse {
      val provider = new XPathCaptureProvider
      providersMap = providersMap + (identifier -> provider)
      provider
    }
  }

  private[capture] class XPathCaptureProvider extends AbstractCaptureProvider {

    var vtdEngine: VTDGen = null
    var vn: VTDNav = null
    var ap: AutoPilot = null

    def captureOne(target: Any, from: Any): Option[String] = {

      if (vtdEngine == null) {
        vtdEngine = new VTDGen
        vtdEngine.setDoc(from match {
          case x: String => x.getBytes
          case x: Array[Byte] => x
        })
        logger.debug("XPATH CAPTURE - DOC SET")
        vtdEngine.parse(false)
        vn = vtdEngine.getNav()
        ap = new AutoPilot(vn)
      }
      ap.selectXPath(target.toString)
      val value = Some(ap.evalXPathToString)
      vtdEngine.clear
      logger.debug("XPATH CAPTURE: {}", value)
      value
    }

    def captureAll(target: Any, from: Any): Option[String] = {
      null
    }
  }
}