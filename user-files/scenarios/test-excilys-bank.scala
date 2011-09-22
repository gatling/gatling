/* URLs */
val urlBase = "http://localhost:8080/excilys-bank-web"
val urlLogin = urlBase + "/login"
val urlAccueil = urlBase + "/private/bank/accounts.html"

/* Feeder */
val usersInfos = new TSVFeeder("bank2", List("username", "password", "acc1", "acc2", "acc3", "acc4"))

/* Scenarios */
include("connection-feeder")

/* Configuration */
val scnConf = configureScenario(scn) withFeeder usersInfos withUsersNumber 100 withRampOf 10

/* Simulation */
runSimulations(scnConf)