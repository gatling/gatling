############
Save body and use it for next request
############

In this scenario, we'll get a certificate and use it to encrypt a credit card number.

When we call "/certificate" we will get this json back.
::
  {
      "id": "",
      "value": "-----BEGIN CERTIFICATE-----\r\n
                Iamsomemumbojumbo
                -----END CERTIFICATE-----\r\n"
  }

Here's the scenario.

::

  import io.gatling.core.Predef._
  import io.gatling.http.Predef._
  import io.gatling.http.request.Body

  class AddBilling extends Simulation {

    def encryptCard(number: String, certificate: String): String = {
      //do encryption and return encrypted credit card
    }

    val appHeader = Map(
    "Content-Type"  -> "application/json"
    )

    val baseHttpConfig = http
      .baseURL("http://localhost:8080")
      .acceptHeader("application/json")
      .connection("keep-alive")
    )

    // SCENARIOS
    val billing = scenario("Add billing")
        .exec(
          http("Get encryption certificate")
          .get("/certificate")
          .headers(appHeader)
          .check(status.is(200),
            jsonPath("$.id").exists.saveAs("certificateId"),
            jsonPath("$.value").exists.saveAs("certificate"))
        )
        .exec(
          session => {
            //encrypt card by getting the certificate from session saved in previous steps
            val encryptedCreditCard = encryptCard("card number", session("certificate").as[String])
            //add the encrypted string to session so we can use in json body in the next request
            session.set("encryptedCreditCard", encryptedCreditCard)
          }
        )
        .exec(
          http("Add billing")
            .post("/billing")
            .headers(appHeader)
            .body(StringBody("""{
                                 "billing_method":
                                     "card":{
                                        "encryption_key":"${certificateId}",
                                        "number":"${encryptedCreditCard}"
                                    }
            }"""))
            .check(status.is(200))
        )
    setUp(billing.inject(atOnceUsers(1))).protocols(baseHttpConfig)
  }
