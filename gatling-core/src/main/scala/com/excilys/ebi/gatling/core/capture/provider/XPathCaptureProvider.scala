package com.excilys.ebi.gatling.core.capture.provider

import com.ximpleware.VTDGen
import com.ximpleware.VTDNav
import com.ximpleware.AutoPilot

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object XPathCaptureProvider {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[XPathCaptureProvider]);
}
class XPathCaptureProvider extends AbstractCaptureProvider {

  def capture(target: Any, from: Any): Option[String] = { null }

  def captureOne(target: Any, from: Any): Option[String] = {
    val vg = new VTDGen

    vg.setDoc(from match {
      case x: String => x.getBytes
      case x: Array[Byte] => x
    })

    vg.parse(false)

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
