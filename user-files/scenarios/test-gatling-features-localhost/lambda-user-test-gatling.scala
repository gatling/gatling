val lambdaUser = scenario("Standard User")
  // First request outside iteration
  .doHttpRequest(
    "Catégorie Poney",
    get(baseUrl))
  .pause(pause3)
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
        assertXpath("//input[@value='aaaa']/@id") in "ctxParam",
        assertRegexp("""<input id="text1" type="text" value="aaaa" />"""),
        assertStatusInRange(200 to 210) in "blablaParam",
        assertXpath("//input[@value='aaaa']/@id", "text1"),
        assertXpath("//input[@id='text1']/@value", "aaaa") in "test2")
      .pause(pause2)
      .doHttpRequest("Url from context",
        get("http://localhost:3000/{}", "test2"))
      .pause(pause1)
      // Second request to be repeated
      .doHttpRequest(
        "Create Thing blabla",
        post("http://localhost:3000/things") withQueryParam "login" withQueryParam "password" withTemplateBody ("create_thing", Map("name" -> "blabla")) asJSON) //,
      //assertRegexp("""<input value="(.*)"/>""", "blabla"))
      .pause(pause1)
      // Third request to be repeated
      .doHttpRequest(
        "Liste Articles",
        get("http://localhost:3000/things") withQueryParam "firstname" withQueryParam "lastname")
      .pause(pause1)
      .doHttpRequest(
        "Test Page",
        get("http://localhost:3000/tests"),
        assertHeader(CONTENT_TYPE, "text/html; charset=utf-8") in "ctxParam")
      // Fourth request to be repeated
      .doHttpRequest(
        "Create Thing omgomg",
        post("http://localhost:3000/things") withQueryParam ("postTest", FromContext("ctxParam")) withTemplateBodyFromContext ("create_thing", Map("name" -> "ctxParam")) asJSON,
        assertStatus(201) in "status"))
  // Second request outside iteration
  .doHttpRequest("Ajout au panier",
    get(baseUrl),
    regexp("""<input id="text1" type="text" value="(.*)" />""") in "input")
  .pause(pause1)