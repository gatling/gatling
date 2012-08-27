package ratelimit
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.ning.http.client.Response
import scala.collection.mutable.ListBuffer
import org.apache.commons.math3.distribution.NormalDistribution

class RateLimitSimulation extends Simulation {
  private var rateCounter = new ListBuffer[Long]()

  private val maxRate = 20
  private var waitMean = 1000L
  private val waitStandardDeviation = 200L

  def apply = {

    val urlBase = "http://excilys-bank-web.cloudfoundry.com"

    val httpConf = httpConfig.baseURL(urlBase).disableFollowRedirect.responseInfoExtractor((response: Response) => {
      val now = System.currentTimeMillis
      rateCounter --= rateCounter.filter(_ < (now - 1000))
      rateCounter.append(now)

      if ( rateCounter.size < maxRate )
      {
        waitMean -= 1
        if ( waitMean < 0 ) waitMean = 0
      }
      else
      {
        waitMean += 2
      }
      Nil
    })

    val headers_1 = Map(
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
      "Accept-Charset" -> "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
      "Accept-Encoding" -> "gzip,deflate",
      "Accept-Language" -> "fr,en-us;q=0.7,en;q=0.3",
      "Host" -> "excilys-bank-web.cloudfoundry.com",
      "Keep-Alive" -> "115")

    val throttlingPause = () => {
      new NormalDistribution(waitMean, waitStandardDeviation).sample().longValue()
    }

    val scn = scenario("Rate Limited Scenario")
      .loop(
      chain.exec(
        http("request_1")
          .get("/")
          .headers(headers_1)
          .check(status.is(302)))
        .pauseCustom(throttlingPause)
    ).times(25)

    List(scn.configure.users(30).ramp(10).protocolConfig(httpConf))
  }
}
