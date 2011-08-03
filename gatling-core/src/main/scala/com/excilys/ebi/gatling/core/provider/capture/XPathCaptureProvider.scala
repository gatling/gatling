package com.excilys.ebi.gatling.core.provider.capture

import com.ximpleware.VTDGen
import com.ximpleware.VTDNav
import com.ximpleware.AutoPilot

class XPathCaptureProvider extends AbstractCaptureProvider {

  var vtdEngines: Map[String, VTDGen] = Map.empty

  def capture(target: Any, from: Any): Option[String] = captureOne(target, from)

  def captureOne(target: Any, from: Any): Option[String] = {

    val mapKey = (from match {
      case x: String => x.substring(0, 3)
      case x: Array[Byte] => x.size.toString
      case _ => throw new IllegalArgumentException
    }) + from.hashCode

    logger.debug("mapKey : {}", mapKey)

    val vg = vtdEngines.get(mapKey).getOrElse {
      val v = new VTDGen
      logger.debug("Created a new Instance of VTDGen")
      vtdEngines = vtdEngines + (mapKey -> v)
      v
    }

    vg.parse(false)

    vg.setDoc(from match {
      case x: String => x.getBytes
      case x: Array[Byte] => x
    })

    val vn = vg.getNav
    val ap = new AutoPilot(vn)
    ap.selectXPath(target.toString)
    val value = Some(ap.evalXPathToString)
    vg.clear
    value
  }

  def captureAll(target: Any, from: Any): Option[String] = {
    null
  }
}
