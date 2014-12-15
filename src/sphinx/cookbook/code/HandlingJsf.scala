import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class HandlingJsf {

  //#factory-methods
  import io.gatling.core.session.Expression

  val jsfViewStateCheck = regex("""="javax.faces.ViewState" value="([^"]*)"""")
    .saveAs("viewState")
  def jsfGet(name: String, url: Expression[String]) = http(name).get(url)
    .check(jsfViewStateCheck)
  def jsfPost(name: String, url: Expression[String]) = http(name).post(url)
    .formParam("javax.faces.ViewState", "${viewState}")
    .check(jsfViewStateCheck)
  //#factory-methods

  //#example-scenario
  val scn = scenario("Scenario Name")
    .exec(jsfGet("request_1", "/showcase-labs/ui/pprUpdate.jsf"))
    .pause(80 milliseconds)
    .exec(
      jsfPost("request_2", "/showcase-labs/ui/pprUpdate.jsf")
        .formParam("javax.faces.partial.ajax", "true")
        .formParam("javax.faces.source", "form:btn")
        .formParam("javax.faces.partial.execute", "@all")
        .formParam("javax.faces.partial.render", "form:display")
        .formParam("form:btn", "form:btn")
        .formParam("form", "form")
        .formParam("form:name", "foo"))
  //#example-scenario

 object Trinidad {
   //#trinidad
   val jsfPageFlowCheck = regex("""\?_afPfm=([^"]*)"""").saveAs("afPfm")
   val jsfViewStateCheck = regex("""="javax.faces.ViewState" value="([^"]*)"""")
     .saveAs("viewState")

   def jsfGet(name: String, url: Expression[String]) = http(name).get(url)
     .check(jsfViewStateCheck)
   def jsfPost(name: String, url: Expression[String]) = http(name).post(url)
     .formParam("javax.faces.ViewState", "${viewState}")
     .check(jsfViewStateCheck).check(jsfPageFlowCheck)

   def trinidadPost(name: String, url: Expression[String]) = http(name).post(url)
     .formParam("javax.faces.ViewState", "${viewState}")
     .queryParam("_afPfm", "${afPfm}")
     .check(jsfViewStateCheck)
     .check(jsfPageFlowCheck)
   def trinidadDownload(name: String, url: Expression[String]) = http(name).post(url)
     .formParam("javax.faces.ViewState", "${viewState}")
     .queryParam("_afPfm", "${afPfm}")
   //#trinidad
 }
}
