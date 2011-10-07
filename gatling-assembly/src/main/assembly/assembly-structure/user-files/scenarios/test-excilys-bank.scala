/* URLs */
//val urlBase = "http://192.168.10.53:8080/excilys-bank-web"
val urlBase = "http://localhost:8080/excilys-bank-web"
val urlLoginGet = urlBase + "/public/login.html"
val urlLoginPost = urlBase + "/login"
val urlHome = urlBase + "/private/bank/accounts.html"
val urlLogout = urlBase + "/logout"

/* Feeder */
val usersInfos = new TSVFeeder("bank2", List("username", "password", "acc1", "acc2", "acc3", "acc4"))

/* Scenarios */
include("users")

/* Configuration */
val scnConf = configureScenarioscn feederusers usersNumber 1200 ramp 300

/* Simulation */
runSimulations(scnConf)