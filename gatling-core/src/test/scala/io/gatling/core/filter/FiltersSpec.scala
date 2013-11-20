package io.gatling.core.filter

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FiltersSpec extends Specification {

	val hosts = List(
		"http://excilys.com",
		"http://ebusinessinformation.fr",
		"http://gatling.io")

	val paths = List(
		"",
		"/assets/images/foo.png",
		"/assets/js/bar.js")

	val urls = for {
		host <- hosts
		path <- paths
	} yield host + path

	val whiteList = WhiteList(List("http://excilys.*"))
	val emptyWhiteList = WhiteList()
	val blackList = BlackList(List("http://.*/assets/.*"))
	val emptyBlackList = BlackList()

	"FiltersHelper" should {

		def isRequestAccepted(filters: Filters, partition: (List[String], List[String])) = {

			val (expectedAccepted, expectedRejected) = partition

			(filters.accept(_: String) must beTrue).foreach(expectedAccepted)
			(filters.accept(_: String) must beFalse).foreach(expectedRejected)
		}

		"filter whitelist correctly when blacklist is empty" in {

			isRequestAccepted(Filters(whiteList, emptyBlackList), urls.partition(_.contains("excilys")))
		}

		"filter whitelist then blacklist when both are specified on whitefirst mode" in {

			isRequestAccepted(Filters(whiteList, blackList), urls.partition { url =>
				url.contains("excilys") && !url.contains("assets")
			})
		}

		"filter blacklist correctly when whitelist is empty" in {

			isRequestAccepted(Filters(blackList, emptyWhiteList), urls.partition { url =>
				!url.contains("assets")
			})
		}

		"filter blacklist then whitelist when both are specified on blackfirst mode" in {

			isRequestAccepted(Filters(blackList, whiteList), urls.partition { url =>
				!url.contains("assets") && url.contains("excilys")
			})
		}
	}
}
