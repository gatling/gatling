val iterations = 10
val pause1 = 1
val pause2 = 2
val pause3 = 3

val baseUrl = "http://localhost:3000"

val usersInformation = new TSVFeeder("user_information", List("login", "password", "firstname", "lastname"))

include("lambda-user-test-gatling")
include("_admin-user-test-gatling")

runSimulations(
    lambdaUser.configure.users(5).ramp(10).feeder(usersInformation)
    , adminUser.configure.users(5).ramp(10).delay(60).feeder(usersInformation)
)