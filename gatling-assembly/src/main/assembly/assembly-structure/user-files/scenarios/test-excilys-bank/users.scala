/* URLs */
val year = "2011"
val month = "9"
// Index starts at 0
val pageOfOperations = "0"
  
val urlAccountOperations = urlBase + "/private/bank/account/{}/operations.html"
val urlAccountOperationsData = urlBase + "/private/bank/account/{}/year/"+year+"/month/"+month+"/page/"+pageOfOperations+"/operations.json"

val urlAccountCards = urlBase + "/private/bank/account/{}/cards/all/operations.html"
val urlAccountCardsData = urlBase + "/private/bank/account/{}/cards/all/year/"+year+"/month/"+month+"/page/"+pageOfOperations+"/operations.json"

val urlAccountCardsPending = urlBase + "/private/bank/account/{}/cards/all/pending/operations.html"
val urlAccountCardsPendingData = urlBase + "/private/bank/account/{}/cards/all/pending/page/"+pageOfOperations+"/operations.json"

val urlAccountTransfers = urlBase + "/private/bank/account/{}/transfers/operations.html"
val urlAccountTransfersData = urlBase + "/private/bank/account/{}/transfers/page/"+pageOfOperations+"/operations.json"

val urlAccountTransferPerform = urlBase + "/private/bank/account/{}/transfers/perform.html"

/* Scenario */
val scn = scenario("User of Excilys Bank")
		// Login page
		// Not yet working (Now working, but not included in the test) 
		/*.doHttpRequest(
		    "Login GET"
		    , get(urlLoginGet)
		    , checkStatus(200)
		)
		.pause(4)*/
		// Authenticating
		.doHttpRequest(
		    "Authenticating"
		    , post(urlLoginPost) withParam ("username", FromContext("username")) withParam ("password", FromContext("password")) 
		    , checkStatus(302)
		)
		
		// Home page
		.doHttpRequest(
		    "Home"
		    , get(urlHome)
		    , checkRegexpExists("""<a href="/excilys-bank-web/logout" class="button blue">Log out</a>""")
		)
		.pause(15)
		
		.iterate(
		    20
		    , chain
				// Operations page
				.doHttpRequest(
					"Operations details"
				    , get(urlAccountOperations, "acc1")
					, checkRegexpExists("""<table class="accountDetails">""")
				)
				// Load operations data
				.doHttpRequest(
				    "Operations data"
				    , get(urlAccountOperationsData, "acc1")
				    , checkStatus(200)
				)
				.pause(4)
				
				// Cards operations page
				.doHttpRequest(
					"Cards details"
				    , get(urlAccountCards, "acc1")
					, checkRegexpExists("""<table class="accountDetails">""")
				)
				// Load cards operations data
				.doHttpRequest(
				    "Cards data"
				    , get(urlAccountCardsData, "acc1")
				    , checkStatus(200)
				)
				.pause(4)
				
				// Cards pending operations page
				.doHttpRequest(
					"Cards pending details"
				    , get(urlAccountCards, "acc1")
					, checkRegexpExists("""<table class="accountDetails">""")
				)
				// Load cards pending operations data
				.doHttpRequest(
				    "Cards pending data"
				    , get(urlAccountCardsData, "acc1")
				    , checkStatus(200)
				)
				.pause(4)
				
				// Transfers page
				.doHttpRequest(
					"Transfers details"
				    , get(urlAccountTransfers, "acc1")
					, checkRegexpExists("""<table class="accountDetails">""")
				)
				// Load transfers data
				.doHttpRequest(
				    "Transfers data"
				    , get(urlAccountTransfersData, "acc1")
				    , checkStatus(200)
				)
				.pause(4)
				
				// Transfer perform page
				.doHttpRequest(
				    "Transfer perform"
				    , get(urlAccountTransferPerform, "acc1")
				)
				.pause(6)
				
				// Transfer performing
				.doHttpRequest(
				    "Transfer performing"
				    , post(urlAccountTransferPerform, "acc1") 
				    	withParam ("debitedAccountNumber", FromContext("acc1")) 
				    	withParam ("creditedAccountNumber", FromContext("acc2")) 
				    	withParam ("amount", "10")
				    , checkStatus(302)
				)
				.pause(4)
		)
		
		// Logout
		.doHttpRequest(
		    "Logging out"
		    , get(urlLogout)
		    , checkStatus(302)
		)