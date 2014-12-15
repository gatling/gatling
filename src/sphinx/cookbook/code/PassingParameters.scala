import io.gatling.core.Predef._

class PassingParameters extends Simulation {

  //#string-property
  val foo = System.getProperty("foo")
  //#string-property

  val scn = scenario("foo")

  //#injection-from-props
  val nbUsers = Integer.getInteger("users", 1)
  val myRamp  = java.lang.Long.getLong("ramp", 0L)
  setUp(scn.inject(rampUsers(nbUsers) over (myRamp seconds)))
  //#injection-from-props
}
