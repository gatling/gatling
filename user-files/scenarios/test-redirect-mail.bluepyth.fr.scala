
val lambdaUser = scenario("Standard User").doHttpRequest("Redirect TEST", get("http://mail.bluepyth.fr") followsRedirect true, assertRegexp("""<div class="boxtitle">(.*)</div>""", "Bienvenue sur BluePyth Webmail"))

val lambdaUserConfig = configureScenario(lambdaUser) withUsersNumber 1

val execution = runSimulations(lambdaUserConfig)
