import io.gatling.javaapi.core.*

import io.gatling.javaapi.core.CoreDsl.*

//#imports
import io.gatling.javaapi.grpc.*

import io.gatling.javaapi.grpc.GrpcDsl.*
//#imports

import io.grpc.*

class GrpcOfficialSampleKotlin : Simulation() {

  init {
    //#protocol
    val grpcProtocol = grpc
      .forAddress("host", 50051)
      .useInsecureTrustManager() // not required, useInsecureTrustManager is the default
    //#protocol
  }

  private class RequestMessage private constructor(val message: String?) {
    data class Builder(var message: String? = null) {
      fun setMessage(message: String?) = apply {
        this.message = message
      }
      fun build() = RequestMessage(message)
    }
    companion object {
      fun newBuilder(): Builder = Builder()
    }
  }

  init {
    //#expression
    val request = { session: Session ->
      val message = session.getString("message")
      RequestMessage.newBuilder()
        .setMessage(message)
        .build()
    }
    //#expression
  }
}
