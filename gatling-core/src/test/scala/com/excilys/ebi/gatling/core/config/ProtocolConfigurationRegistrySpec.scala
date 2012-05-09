package com.excilys.ebi.gatling.core.config

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProtocolConfigurationRegistrySpec extends Specification {

	def newConfig(theType: String) = new ProtocolConfiguration {
		val protocolType = theType
	}

	"building registry" should {

		"return the configuration when 1 configuration" in {
			ProtocolConfigurationRegistry(List(newConfig("foo"))).getProtocolConfiguration("foo") must beSome.which(_.protocolType == "foo")
		}

		"return the configurations when 2 different configuration" in {
			val registry = ProtocolConfigurationRegistry(List(newConfig("foo"), newConfig("bar")))
			registry.getProtocolConfiguration("foo") must beSome.which(_.protocolType == "foo")
			registry.getProtocolConfiguration("bar") must beSome.which(_.protocolType == "bar")
		}

		"not fail when no configuration" in {
			ProtocolConfigurationRegistry(List.empty).getProtocolConfiguration("foo") must beNone
		}

		"fail when multiple configurations of the same type" in {
			ProtocolConfigurationRegistry(List(newConfig("foo"), newConfig("foo"))) must throwA[ExceptionInInitializerError]
		}
	}
}