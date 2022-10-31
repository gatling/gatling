package carrier.requestsjava;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class FloodIORequestsJava {
    public static Map<String, String> headers_5 = Map.of(
            "Accept-Encoding", "gzip, deflate",
            "Pragma", "no-cache",
            "Host", "challengers.flood.io",
            "Origin", "https://challengers.flood.io",
            "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "X-Requested-With", "XMLHttpRequest",
            "Upgrade-Insecure-Requests", "1");

    public  static ChainBuilder dataJSON = exec(http("Step5_GET_Code")
            .get("/code")
            .check(jsonPath("$.code").saveAs("dataJSON")));

    public static ChainBuilder Step1GET = exec(http("Step1_GET")
            .get("/")
            .headers(headers_5)
            .check(status().is(200))
            .check(regex("authenticity_token.*?value=\"(.*?)\"").find().saveAs("token"))
            .check(regex("step_id.+?value=\"(.+?)\"").find().saveAs("challenger"))
            .check(regex("step_number\".*?value=\"(.*?)\"").find().saveAs("stepNumber")))
            .pause(1);

    public static ChainBuilder Step1POST = exec(http("Step1_POST")
            .post("/start")
            .headers(headers_5)
            .formParam("utf8", "✓")
            .formParam("authenticity_token", "${token}")
            .formParam("challenger[step_id]", "${challenger}")
            .formParam("challenger[step_number]", "${stepNumber}")
            .formParam("commit", "Start"))
            .pause(1);

    public static ChainBuilder Step2GET = exec(http("Step2_GET")
            .get("/step/2")
            .headers(headers_5)
            .check(regex("step_id.+?value=\"(.+?)\"").find().saveAs("challenger2"))
            .check(regex("step_number\".*?value=\"(.*?)\"").find().saveAs("stepNumber2")))
            .pause(1);

    public static ChainBuilder Step2POST =
            exec(http("Step2_POST")
                    .post("/start")
                    .headers(headers_5)
                    .formParam("utf8", "✓")
                    .formParam("authenticity_token", "${token}")
                    .formParam("challenger[step_id]", "${challenger2}")
                    .formParam("challenger[step_number]", "${stepNumber2}")
                    .formParam("challenger[age]", "18")
                    .formParam("commit", "Next"))
                    .pause(1);



    public static ChainBuilder Step3GET =
            exec(http("Step3_GET")
                    .get("/step/3")
                    .headers(headers_5)
                    .check(regex("step_id.+?value=\"(.+?)\"").find().saveAs("challenger3"))
                    .check(regex("step_number\".*?value=\"(.*?)\"").find().saveAs("stepNumber3"))
                    .check(regex("challenger_order_selected_.+?\">(.+?)<\\/label>").findAll().saveAs("number"))
                    .check(regex("radio\" value=\"(.+?)\"").findAll().saveAs("order_selected")))
                    .exec(session -> {
                        List<String> numbers = session.getList("number");
                        List<Integer> numInt = numbers.stream().map(String::toString).map(Integer::valueOf).collect(Collectors.toList());
                        List<String> buttons = session.getList("order_selected");
                        Map<Integer, String> map = IntStream.range(0, numInt.size()).boxed()
                                .collect(Collectors.toMap(numInt::get, buttons::get));
                        System.out.println(Arrays.asList(map));
                        int max = Collections.max(map.keySet());
                        System.out.println("max: " + max);
                        String button = map.get(max);
                        System.out.println("button: " + button);
                        System.out.println("Session before:");
                        System.out.println(Arrays.asList(session.asScala().attributes()));
                        session.set("num", max);
                        session.set("order", button);
                        System.out.println("Session after:");
                        System.out.println(Arrays.asList(session.asScala().attributes()));
                        System.out.println("num from session: " + session.get("num"));
                        System.out.println("order from session: " + session.get("order"));
                        return session.set("num", max).set("order", button);
                    })
                    .exec(session -> {
                        System.out.println("Session next step:");
                        System.out.println(Arrays.asList(session.asScala().attributes()));
                        System.out.println("num from session next step: " + session.get("num"));
                        System.out.println("order from session next step: " + session.get("order"));
                        return session;
                    })
                    .pause(1);

    public static ChainBuilder Step3POST = exec(http("Step3_POST")
            .post("/start")
            .headers(headers_5)
            .formParam("utf8", "✓")
            .formParam("authenticity_token", "${token}")
            .formParam("challenger[step_id]", "${challenger3}")
            .formParam("challenger[step_number]", "${stepNumber3}")
            .formParam("challenger[largest_order]", "${num}")
            .formParam("challenger[order_selected]", "${order}")
            .formParam("commit", "Next"))
            .pause(1);

    public static ChainBuilder Step4GET = exec(http("Step4_GET")
            .get("/step/4")
            .headers(headers_5)
            .check(regex("step_id.+?value=\"(.+?)\"").find().saveAs("challenger4"))
            .check(regex("step_number\".*?value=\"(.*?)\"").find().saveAs("stepNumber4"))
            .check(regex("challenger_order_.+? name=\"(.+?)\".+?value=\".+?\"").findAll().saveAs("orderName"))
            .check(regex("challenger_order_.+? name=\".+?\".+?value=\"(.+?)\"").find().saveAs("orderValue")))
            .exec(session -> {
                List<String> orderName = session.getList("orderName");
                return session
                        .set("orderName_1", orderName.get(0))
                        .set("orderName_2", orderName.get(1))
                        .set("orderName_3", orderName.get(2))
                        .set("orderName_4", orderName.get(3))
                        .set("orderName_5", orderName.get(4))
                        .set("orderName_6", orderName.get(5))
                        .set("orderName_7", orderName.get(6))
                        .set("orderName_8", orderName.get(7))
                        .set("orderName_9", orderName.get(8))
                        .set("orderName_10", orderName.get(9));
            })
            .pause(1);

    public static ChainBuilder Step4POST = exec(http("Step4_POST")
            .post("/start")
            .headers(headers_5)
            .formParam("utf8", "✓")
            .formParam("authenticity_token", "${token}")
            .formParam("challenger[step_id]", "${challenger4}")
            .formParam("challenger[step_number]", "${stepNumber4}")
            .formParam("${orderName_1}", "${orderValue}")
            .formParam("${orderName_2}", "${orderValue}")
            .formParam("${orderName_3}", "${orderValue}")
            .formParam("${orderName_4}", "${orderValue}")
            .formParam("${orderName_5}", "${orderValue}")
            .formParam("${orderName_6}", "${orderValue}")
            .formParam("${orderName_7}", "${orderValue}")
            .formParam("${orderName_8}", "${orderValue}")
            .formParam("${orderName_9}", "${orderValue}")
            .formParam("${orderName_10}", "${orderValue}")
            .formParam("commit", "Next"));

    public static ChainBuilder Step5GET = exec(http("Step5_GET")
            .get("/step/5")
            .headers(headers_5)
            .check(regex("step_id.+?value=\"(.+?)\"").find().saveAs("challenger5"))
            .check(regex("step_number\".*?value=\"(.*?)\"").find().saveAs("stepNumber5")));

    public static ChainBuilder Step5POST = exec(http("Step5_POST")
            .post("/start")
            .headers(headers_5)
            .formParam("utf8", "✓")
            .formParam("authenticity_token", "${token}")
            .formParam("challenger[step_id]", "${challenger5}")
            .formParam("challenger[step_number]", "${stepNumber5}")
            .formParam("challenger[one_time_token]", "${dataJSON}")
            .formParam("commit", "Next"));

    public static ChainBuilder FinalStep = exec(http("Final_Step")
            .get("/done")
            .headers(headers_5)
            .check(regex("You're Done!")));

    public static ChainBuilder failedFinalStep = exec(http("Final_Step")
            .get("/done")
            .headers(headers_5)
            .queryParam("milestone", "1")
            .queryParam("state", "open")
            .check(regex("JAVA You're Done!!!")));

    }
