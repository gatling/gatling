val adminUser = scenario("Admin User")
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
        "Page Admin",
        get(baseUrl) withFeeder usersInformation withQueryParam "firstname")
      .pause(pause2))