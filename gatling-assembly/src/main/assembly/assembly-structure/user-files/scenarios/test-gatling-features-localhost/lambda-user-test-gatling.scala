val loginChain = chain.doHttpRequest("First Request Chain", get(baseUrl)).pause(1,2)

val loginGroup = "Login"
val doStuffGroup = "Do Stuff"


val lambdaUser = scenario("Standard User")
  .insertChain(loginChain)
  // First request outside iteration
  .doHttpRequest(
    "Catégorie Poney",
    get(baseUrl),
    captureXpath("//input[@id='text1']/@value") in "aaaa_value")
  .pause(pause2, pause3)
  .doFor(12000, TimeUnit.MILLISECONDS,
      chain
        .doHttpRequest("In During 1", get(baseUrl))
        .pause(2)
        .doHttpRequest("In During 2", get(baseUrl))
        .pause(2))
  // Loop
  .iterate(
    // How many times ?
    iterations,
    // What will be repeated ?
    chain
      // First request to be repeated
      .doHttpRequest(
        "Page accueil",
        get(baseUrl),
        checkXpathExists(interpolate("//input[@value='{}']/@id", "aaaa_value")) in "ctxParam",
        checkXpathNotExists(interpolate("//input[@id='{}']/@value", "aaaa_value")) in "ctxParam2",
        checkRegexpExists("""<input id="text1" type="text" value="aaaa" />"""),
        checkRegexpNotExists("""<input id="text1" type="test" value="aaaa" />"""),
        checkStatusInRange(200 to 210) in "blablaParam",
        checkXpathNotEquals("//input[@value='aaaa']/@id", "omg"),
        checkXpathEquals("//input[@id='text1']/@value", "aaaa") in "test2")
      .pause(pause2)
      .startGroup(loginGroup)
      .doIf("test2", "aaaa", 
          chain.doHttpRequest("IF=TRUE Request", get(baseUrl))
          //, chain.doHttpRequest("IF=FALSE AAAA Request", get(baseUrl))
          )
      .pause(pause2)
      .doHttpRequest("Url from context",
        get("http://localhost:3000/{}", "test2"))
      .pause(1000, 3000, TimeUnit.MILLISECONDS)
      // Second request to be repeated
      .doHttpRequest(
        "Create Thing blabla",
        post("http://localhost:3000/things") withQueryParam "login" withQueryParam "password" withTemplateBody ("create_thing", Map("name" -> "blabla")) asJSON) //,
      //checkRegexpEquals("""<input value="(.*)"/>""", "blabla"))
      .pause(pause1)
      .endGroup(loginGroup)
      // Third request to be repeated
      .doHttpRequest(
        "Liste Articles",
        get("http://localhost:3000/things") withQueryParam "firstname" withQueryParam "lastname")
      .pause(pause1)
      .doHttpRequest(
        "Test Page",
        get("http://localhost:3000/tests"),
        checkHeaderEquals(CONTENT_TYPE, "text/html; charset=utf-8") in "ctxParam")
      // Fourth request to be repeated
      .doHttpRequest(
        "Create Thing omgomg",
        post("http://localhost:3000/things") withQueryParam ("postTest", FromContext("ctxParam")) withTemplateBody ("create_thing", Map("name" -> FromContext("ctxParam"))) asJSON,
        checkStatus(201) in "status"))
  // Second request outside iteration
  .startGroup(doStuffGroup)
  .doHttpRequest("Ajout au panier",
    get(baseUrl),
    captureRegexp("""<input id="text1" type="text" value="(.*)" />""") in "input")
  .pause(pause1)
  .endGroup(doStuffGroup)