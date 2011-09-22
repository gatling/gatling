/* URLs */
val urlFirstAccount = urlBase + "/private/bank/account/{}/operations.html"
val urlFirstAccountTransfer = urlBase + "/private/bank/account/{}/transfers/operations.html"
val urlFirstAccountTransferPerform = urlBase + "/private/bank/account/{}/transfers/perform.html"

/* Scenario */
val scn = scenario("Test Excilys Bank")
		.doHttpRequest(
		    "Login"
		    , post(urlLogin) withParam ("username", FromContext("username")) withParam ("password", FromContext("password")) 
		    , assertStatus(302)
		)
		.doHttpRequest(
		    "Home"
		    , get(urlAccueil)
		    , assertRegexp("""<a href="/excilys-bank-web/logout" class="button blue">Log out</a>""")
		)
		.pause(4)
		.doHttpRequest(
			"Account details"
		    , get(urlFirstAccount, "acc1")
			, assertRegexp("""<table class="accountDetails">""")
		)
		.pause(4)
		.doHttpRequest(
			"Transfers"
		    , get(urlFirstAccountTransfer, "acc1")
			, assertRegexp("""<table class="accountDetails">""")
		)
		.pause(4)
		.doHttpRequest(
			"Perform transfer"
		    , get(urlFirstAccountTransferPerform, "acc1")
			, assertRegexp("""<legend>Perform a new transfer</legend>""")
		)
		.pause(4)
		.iterate(
		    20
		    , chain
				.doHttpRequest(
				    "Transfer performing"
				    , post(urlFirstAccountTransferPerform, "acc1") followsRedirect true 
				    	withParam ("debitedAccountNumber", FromContext("acc1")) 
				    	withParam ("creditedAccountNumber", FromContext("acc2")) 
				    	withParam ("amount", "10")
				)
				.pause(2)
		)