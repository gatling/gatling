val sc = scenario("Test Basic AUTH").exec(http("Protected").get("http://dev.bluepyth.fr").basicAuth("test", "test"))

runSimulations(sc.configure.users(1))