val iterations = 10
val pause1 = 3
val pause2 = 2
val pause3 = 1

val url = "http://localhost:3000"

val usersCredentials = new TSVFeeder("user_credential", List("login", "password"))
val usersInformation = new TSVFeeder("user_information", List("firstname", "lastname"))

val lambdaUser =
  scenario("Standard User")
    // First request outside iteration
    .doHttpRequest(
      "Catégorie Poney",
      get(url))
    .pause(pause1)
    // Loop
    .iterate(
      // How many times ?
      iterations,
      // What will be repeated ?
      chain
        // First request to be repeated
        .doHttpRequest(
          "Page accueil",
          get(url),
          assertXpath("//input[@value='aaaa']/@id", "text1") in "ctxParam",
          assertStatusInRange(200 to 210) in "blablaParam",
          assertXpath("//input[@value='aaaa']/@id", "text1") in "test1",
          assertXpath("//input[@id='text1']/@value", "aaaa") in "test2")
        .pause(pause2)
        // Second request to be repeated
        .doHttpRequest(
          "Create Thing blabla",
          post("http://localhost:3000/things") followsRedirect true withFeeder usersCredentials withQueryParam "login" withQueryParam "password" withTemplateBody ("create_thing", Map("name" -> "blabla")) asJSON)//,
          //assertRegexp("""<input value="(.*)"/>""", "blabla"))
        .pause(pause3)
        // Third request to be repeated
        .doHttpRequest(
          "Liste Articles",
          get("http://localhost:3000/things") withFeeder usersInformation withQueryParam "firstname" withQueryParam "lastname")
        .pause(pause3)
        .doHttpRequest(
          "Test Page",
          get("http://localhost:3000/tests"),
          assertHeader(ContentType, "text/html; charset=utf-8") in "ctxParam")
        // Fourth request to be repeated
        .doHttpRequest(
          "Create Thing omgomg",
          post("http://localhost:3000/things") withQueryParam ("postTest", FromContext("ctxParam")) withTemplateBodyFromContext ("create_thing", Map("name" -> "ctxParam")) asJSON,
          assertStatus(201)))
    // Second request outside iteration
    .doHttpRequest("Ajout au panier",
      get(url),
      regexp("""<input id="text1" type="text" value="(.*)" />""") in "input")
    .pause(pause3)
    
    
val adminUser =
  scenario("Admin User")
    // First request outside iteration
    .doHttpRequest(
      "Catégorie Poney",
      get(url))
    .pause(pause1)
    // Loop
    .iterate(
      // How many times ?
      iterations,
      // What will be repeated ?
      chain
        // First request to be repeated
        .doHttpRequest(
          "Page Admin",
          get(url) withFeeder usersInformation withQueryParam "firstname")
        .pause(pause2)
    )


  val lambdaUserConfig = configureScenario(lambdaUser) withUsersNumber 5 withRampOf 10
  val adminConfig = configureScenario(adminUser) withUsersNumber 5 withRampOf (5000, TimeUnit.MILLISECONDS) startsAt 130
  
  val execution = runSimulations(lambdaUserConfig, adminConfig)
