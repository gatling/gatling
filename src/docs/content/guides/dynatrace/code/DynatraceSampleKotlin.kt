import io.gatling.http.client.Request
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.http.HttpDsl.*
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DynatraceSampleKotlin {

//#dynatrace
private val Hostname: String = InetAddress.getLocalHost().getHostName()

// Source Id identifies the product that triggered the request
private val SI = "GATLING"

// The Load Test Name uniquely identifies a test execution
private val LTN =
  javaClass.simpleName +
  "_" +
  LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

val httpProtocol = http
  .sign { request: Request, session: Session ->
    // Virtual User ID of the unique user who sent the request
    val VU = "${Hostname}_${session.scenario()}_${session.userId()}"

    // Test Step Name is a logical test step within your load testing script
    val TSN = request.name

    // Load Script Name - name of the load testing script.
    val LSN = session.scenario()

    // Page Context provides information about the document
    val PC = session.groups().joinToString(",")

    request.headers["x-dynaTrace"] =
      "VU=$VU;SI=$SI;TSN=$TSN;LSN=$LSN;LTN=$LTN;PC=$PC"

    request
  }
//#dynatrace
}