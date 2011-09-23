
val lambdaUser = scenario("Standard User").doHttpRequest("Redirect TEST", get("http://mail.bluepyth.fr") followsRedirect true, checkRegexpExists("""<div class="boxtitle">(.*)</div>""", "Bienvenue sur BluePyth Webmail"))

val lambdaUserConfig = configureScenario(lambdaUser) withUsersNumber 1

runSimulations(lambdaUserConfig)
