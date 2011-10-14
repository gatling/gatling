val adminUser = scenario("Admin User")
  // First request outside iteration
  .exec( http("Catégorie Poney") get(baseUrl))
  .pause( pause3 )
  // Loop
  .iterate(
    // How many times ?
    iterations,
    // What will be repeated ?
    chain
      // First request to be repeated
      .exec( http("Page Admin") get(baseUrl) queryParam "firstname")
      .pause( pause2 )
  )