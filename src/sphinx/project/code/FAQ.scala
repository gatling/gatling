import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder

class FAQ {

  //#chains
  object ChainLibrary1 {
    val chain1: ChainBuilder = ???
    val chain2: ChainBuilder = ???
    // etc...
    val chain100: ChainBuilder = ???
  }

  object ChainLibrary2 {
    val chain101: ChainBuilder = ???
    val chain102: ChainBuilder = ???
    // etc...
    val chain150: ChainBuilder = ???
  }

  class MyVeryBigSimulation {

    import ChainLibrary1._
    import ChainLibrary2._

    val scn = scenario("Name")
      .exec(chain1, chain2,/* etc... */ chain100)
      .exec(chain101, chain102,/* etc... */ chain150)
  }
  //#chains
}
