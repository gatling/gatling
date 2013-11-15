package io.gatling.recorder.util

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import io.gatling.core.filter.{ BlackList, WhiteList }
import io.gatling.recorder.config.{ FiltersConfiguration, RecorderConfiguration }
import io.gatling.recorder.config.RecorderConfiguration.{ configuration, configuration_= => configuration_= }
import io.gatling.recorder.enumeration.FilterStrategy

@RunWith(classOf[JUnitRunner])
class FiltersHelperSpec extends Specification {

	sequential

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

	val whitelist = WhiteList(List("http://excilys.*"))
	val blacklist = BlackList(List("http://.*/assets/.*"))

	implicit class RecorderConfigurationEnhanced(configuration: RecorderConfiguration) {

		def filterAndCheck(filterStrategy: FilterStrategy.Value) = {
			val tmpConf = configuration.copy(
				filters = configuration.filters.copy(
					filterStrategy = filterStrategy))

			tmpConf.filters.filterStrategy must be equalTo filterStrategy

			tmpConf
		}

		def listsAndCheck(whitelist: WhiteList, blacklist: BlackList) = {
			val tmpConf = configuration.copy(
				filters = configuration.filters.copy(
					whiteList = whitelist,
					blackList = blacklist))

			tmpConf.filters.whiteList must be equalTo whitelist
			tmpConf.filters.blackList must be equalTo blacklist

			tmpConf
		}
	}

	val defaults = RecorderConfiguration(
		filters = FiltersConfiguration(
			filterStrategy = FilterStrategy.DISABLED,
			whiteList = whitelist,
			blackList = blacklist),
		http = null,
		proxy = null,
		core = null,
		config = null)

	"FiltersHelper" should {

		"be in the right state by default" in {
			configuration = defaults

			configuration.filters.filterStrategy must be equalTo FilterStrategy.DISABLED
			configuration.filters.whiteList must be equalTo whitelist
			configuration.filters.blackList must be equalTo blacklist
		}

		"not filter anything while disabled" in {
			configuration = defaults.filterAndCheck(FilterStrategy.DISABLED)

			(FiltersHelper.isRequestAccepted((_: String)) must beTrue).foreach(urls)
		}

		def isRequestAccepted(partition: (List[String], List[String])) = {
			val (valid, invalid) = partition

			(FiltersHelper.isRequestAccepted((_: String)) must beTrue).foreach(valid)
			(FiltersHelper.isRequestAccepted((_: String)) must beFalse).foreach(invalid)
		}

		"filter whitelist correctly when blacklist is empty" in {
			val tmpConf = defaults.filterAndCheck(FilterStrategy.WHITELIST_FIRST)
			configuration = tmpConf.listsAndCheck(whitelist, BlackList(List.empty))

			isRequestAccepted(urls.partition { url =>
				url.contains("excilys")
			})
		}

		"filter whitelist then blacklist when both are specified on whitefirst mode" in {
			configuration = defaults.filterAndCheck(FilterStrategy.WHITELIST_FIRST)

			isRequestAccepted(urls.partition { url =>
				url.contains("excilys") && !url.contains("assets")
			})
		}

		"filter blacklist correctly when whitelist is empty" in {
			val tmpConf = defaults.filterAndCheck(FilterStrategy.BLACKLIST_FIRST)
			configuration = tmpConf.listsAndCheck(WhiteList(List.empty), blacklist)

			isRequestAccepted(urls.partition { url =>
				!url.contains("assets")
			})
		}

		"filter blacklist then whitelist when both are specified on blackfirst mode" in {
			configuration = defaults.filterAndCheck(FilterStrategy.BLACKLIST_FIRST)

			isRequestAccepted(urls.partition { url =>
				!url.contains("assets") && url.contains("excilys")
			})
		}
	}
}
