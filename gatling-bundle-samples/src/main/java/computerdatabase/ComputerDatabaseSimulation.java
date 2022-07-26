package computerdatabase;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * This sample is based on our official tutorials:
 * <ul>
 *   <li><a href="https://gatling.io/docs/gatling/tutorials/quickstart">Gatling quickstart tutorial</a>
 *   <li><a href="https://gatling.io/docs/gatling/tutorials/advanced">Gatling advanced tutorial</a>
 * </ul>
 */
public class ComputerDatabaseSimulation extends Simulation {

    FeederBuilder<String> feeder = csv("search.csv").random();

    ChainBuilder search =
        exec(http("Home").get("/"))
            .pause(1)
            .feed(feeder)
            .exec(
                http("Search")
                    .get("/computers?f=#{searchCriterion}")
                    .check(
                        css("a:contains('#{searchComputerName}')", "href").saveAs("computerUrl")
                    )
            )
            .pause(1)
            .exec(
                http("Select")
                    .get("#{computerUrl}")
                    .check(status().is(200))
            )
            .pause(1);

    // Repeat is a loop resolved at RUNTIME
    ChainBuilder browse =
        // Note how we force the counter name, so we can reuse it
        repeat(4, "i").on(
            exec(
                http("Page #{i}")
                    .get("/computers?p=#{i}")
            ).pause(1)
        );

    // Note we should be using a feeder here
    // Let's demonstrate how we can retry: let's make the request fail randomly and retry a given
    // number of times
    ChainBuilder edit =
        // Let's try at max 2 times
        tryMax(2)
            .on(
                exec(
                    http("Form")
                        .get("/computers/new")
                )
                    .pause(1)
                    .exec(
                        http("Post")
                            .post("/computers")
                            .formParam("name", "Beautiful Computer")
                            .formParam("introduced", "2012-05-30")
                            .formParam("discontinued", "")
                            .formParam("company", "37")
                            .check(
                                status().is(
                                    // We do a check on a condition that's been customized with
                                    // a lambda. It will be evaluated every time a user executes
                                    // the request.
                                    session -> 200 + ThreadLocalRandom.current().nextInt(2)
                                )
                            )
                    )
            )
            // If the chain didn't finally succeed, have the user exit the whole scenario
            .exitHereIfFailed();

    HttpProtocolBuilder httpProtocol =
        http.baseUrl("https://computer-database.gatling.io")
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .acceptEncodingHeader("gzip, deflate")
            .userAgentHeader(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0"
            );

    ScenarioBuilder users = scenario("Users").exec(search, browse);
    ScenarioBuilder admins = scenario("Admins").exec(search, browse, edit);

    {
        setUp(
            users.injectOpen(rampUsers(10).during(10)),
            admins.injectOpen(rampUsers(2).during(10))
        ).protocols(httpProtocol);
    }
}
