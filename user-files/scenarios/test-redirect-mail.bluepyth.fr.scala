val concurrentUsers = 1

val url = "http://mail.bluepyth.fr"
val pause1 = 3

val lambdaUser = scenario("Standard User").doHttpRequest("Redirect TEST", get(url) followsRedirect true, assertRegexp("""<div class="boxtitle">(.*)</div>""", "Bienvenue sur BluePyth Webmail") build).pause(pause1)

val lambdaUserConfig = configureScenario(lambdaUser) withUsersNumber concurrentUsers withRamp (10000, TimeUnit.MILLISECONDS)

val execution = runSimulations(lambdaUserConfig)
