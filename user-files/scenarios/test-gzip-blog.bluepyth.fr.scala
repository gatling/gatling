val sc = scenario("Test Gzip").doHttpRequest("Blog Main Page", get("http://blog.bluepyth.fr"), assertRegexp("<title>(.*)</title>","Blog de BluePyth"))

val scConfiguration = configureScenario(sc) withUsersNumber 1

runSimulations(scConfiguration)