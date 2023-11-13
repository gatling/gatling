import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.gatling.javaapi.http.HttpDsl.http;

class DynatraceSampleJava {

//#dynatrace
private static final String Hostname;

static {
  try {
    Hostname = InetAddress.getLocalHost().getHostName();
  } catch (UnknownHostException e) {
    throw new ExceptionInInitializerError(e);
  }
}

// Source Id identifies the product that triggered the request
private static final String SI = "GATLING";

// The Load Test Name uniquely identifies a test execution
private final String LTN =
  getClass().getSimpleName() +
    "_" +
    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

HttpProtocolBuilder httpProtocol = http
  .sign((request, session) -> {
      // Virtual User ID of the unique user who sent the request
      String VU = Hostname + "_" + session.scenario() + "_" + session.userId();

      // Test Step Name is a logical test step within your load testing script
      String TSN = request.getName();

      // Load Script Name - name of the load testing script.
      String LSN = session.scenario();

      // Page Context provides information about the document
      String PC = String.join(",", session.groups());

      request.getHeaders()
        .set(
          "x-dynaTrace",
          "VU=" + VU + ";SI=" + SI + ";TSN=" + TSN + ";LSN=" + LSN + ";LTN=" + LTN + ";PC=" + PC
        );

      return request;
    }
  );
//#dynatrace
}
