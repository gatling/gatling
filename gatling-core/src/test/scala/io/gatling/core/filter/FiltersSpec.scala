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
		"/infos.html",
		"/assets/images/foo.png",
		"/assets/js/bar.js")

	val urls = for {
		host <- hosts
		path <- paths
	} yield host + path

	val whiteList = WhiteList(List("http://excilys\\.com.*"))
	val emptyWhiteList = WhiteList()
	val blackList = BlackList(List("http://.*/assets/.*"))
	val emptyBlackList = BlackList()

	"Filters" should {

		def isRequestAccepted(filters: Filters, partition: (List[String], List[String])) = {
			val (expectedAccepted, expectedRejected) = partition

			(filters.accept(_: String) must beTrue).foreach(expectedAccepted) and (
				(filters.accept(_: String) must beFalse).foreach(expectedRejected))
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

		"filter correctly when there are multiple patterns" in {
			val patterns = List(".*foo.*", ".*bar.*")
			val url = "http://gatling.io/foo.html"

			BlackList(patterns).accept(url) must beFalse and (WhiteList(patterns).accept(url) must beTrue)
		}

		"filter correctly when there are no patterns" in {
			val url = "http://gatling.io/foo.html"
			BlackList(Nil).accept(url) must beTrue and (WhiteList(Nil).accept(url) must beTrue)
		}

		"be able to deal with incorrect patterns" in {
			val w = WhiteList(List("http://foo\\.com.*", "},{"))
			(w.regexes must not beEmpty) and (w.accept("http://foo.com/bar.html") must beTrue)
		}
	}
}
