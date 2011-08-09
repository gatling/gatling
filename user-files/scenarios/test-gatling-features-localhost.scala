val iterations = 10
val pause1 = 1
val pause2 = 2
val pause3 = 3

val baseUrl = "http://localhost:3000"

val usersCredentials = new TSVFeeder("user_credential", List("login", "password"))
val usersInformation = new TSVFeeder("user_information", List("firstname", "lastname"))

val lambdaUser = scenarioFromFile("lambda-user-test-gatling")
val adminUser = scenarioFromFile("admin-user-test-gatling")

val lambdaUserConfig = configureScenario(lambdaUser) withUsersNumber 5 withRampOf 10
val adminConfig = configureScenario(adminUser) withUsersNumber 5 withRampOf (5000, TimeUnit.MILLISECONDS) startsAt 130

val execution = runSimulations(lambdaUserConfig, adminConfig)
