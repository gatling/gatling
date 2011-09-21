val iterations = 10
val pause1 = 1
val pause2 = 2
val pause3 = 3

val baseUrl = "http://localhost:3000"

val usersInformation = new TSVFeeder("user_information", List("login", "password", "firstname", "lastname"))

include("lambda-user-test-gatling")
include("_admin-user-test-gatling")

val lambdaUserConfig = configureScenario(lambdaUser) withUsersNumber 5 withRampOf 10 withFeeder usersInformation
val adminConfig = configureScenario(adminUser) withUsersNumber 5 withRampOf 10 startsAt 60 withFeeder usersInformation 

runSimulations(lambdaUserConfig, adminConfig)