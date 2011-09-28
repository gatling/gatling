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

val headers = Map(	"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
					"Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
					"Accept-Language" -> "fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4",
					"Host" -> "localhost:8080",
					"Referer" -> "http://localhost:8080/excilys-bank-web/private/bank/account/ACC7/cards/CARD5/year/2011/month/7/operations.html",
					"User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.186 Safari/535.1")

/* Scenario */
val scn = scenario("User of Excilys Bank")
		// Login page
		.doHttpRequest(
		    "Login GET",
		    get(urlLoginGet) withHeaders(headers),
		    checkStatus(200)
		)
		.pause(5, 6)
		// Authenticating
		.doHttpRequest(
		    "Authenticating",
		    post(urlLoginPost) withParam ("username", FromContext("username")) withParam ("password", FromContext("password")) withHeaders(headers),
		    checkStatus(302)
		)
		// Home page
		.doHttpRequest(
		    "Home",
		    get(urlHome) withHeaders(headers),
		    checkRegexpExists("""<a href="/excilys-bank-web/logout" class="button blue">Log out</a>""")
		) 
		.pause(5, 6)
		.iterate(
		    20,
		    chain
				// Operations page
				.doHttpRequest(
					"Operations details",
					get(urlAccountOperations, "acc1") withHeaders(headers),
					checkRegexpExists("""<table class="accountDetails">""")
				)
				// Load operations data
				.doHttpRequest(
				    "Operations data",
				    get(urlAccountOperationsData, "acc1") withHeaders(headers),
				    checkStatus(200)
				)
				.pause(5, 6)
				
				// Cards operations page
				.doHttpRequest(
					"Cards details",
					get(urlAccountCards, "acc1") withHeaders(headers),
					checkRegexpExists("""<table class="accountDetails">""")
				)
				// Load cards operations data
				.doHttpRequest(
				    "Cards data",
				    get(urlAccountCardsData, "acc1")
				    , checkStatus(200)
				)
				.pause(5, 6)
				
				// Cards pending operations page
				.doHttpRequest(
					"Cards pending details",
					get(urlAccountCardsPending, "acc1") withHeaders(headers),
					checkRegexpExists("""<table class="accountDetails">""")
				)
				// Load cards pending operations data
				.doHttpRequest(
				    "Cards pending data",
				    get(urlAccountCardsPendingData, "acc1") withHeaders(headers),
				    checkStatus(200)
				)
				.pause(5, 6)
				
				// Transfers page
				.doHttpRequest(
					"Transfers details",
					get(urlAccountTransfers, "acc1") withHeaders(headers),
					checkRegexpExists("""<table class="accountDetails">""")
				)
				// Load transfers data
				.doHttpRequest(
				    "Transfers data",
				    get(urlAccountTransfersData, "acc1") withHeaders(headers),
				    checkStatus(200)
				)
				.pause(5, 6)
				
				// Transfer perform page
				.doHttpRequest(
				    "Transfer perform",
				    get(urlAccountTransferPerform, "acc1") withHeaders(headers)
				)
				.pause(5, 6)
				
				// Transfer performing
				.doHttpRequest(
				    "Transfer performing",
				    post(urlAccountTransferPerform, "acc1") 
				    	withParam ("debitedAccountNumber", FromContext("acc1")) 
				    	withParam ("creditedAccountNumber", FromContext("acc2")) 
				    	withParam ("amount", "10")
				    	withHeaders(headers),
				    checkStatus(302)
				)
				.pause(5, 6)
		)
		
		// Logout
		.doHttpRequest(
		    "Logging out",
		    get(urlLogout) withHeaders(headers),
		    checkStatus(302)
		)