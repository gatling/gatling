/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#imports
import io.gatling.javaapi.grpc.*;

import static io.gatling.javaapi.grpc.GrpcDsl.*;
//#imports

import java.util.function.Function;

import io.gatling.javaapi.core.*;

import io.grpc.*;

import static io.gatling.javaapi.core.CoreDsl.*;

class GrpcOfficialSampleJava extends Simulation {

  {
    //#protocol
    GrpcProtocolBuilder grpcProtocol = grpc
      .forAddress("host", 50051)
      .useInsecureTrustManager(); // not required, useInsecureTrustManager is the default
    //#protocol
  }

  private static class RequestMessage {
    private final String message;
    private RequestMessage(String message) {
      this.message = message;
    }
    public String getMessage() {
      return message;
    }
    public static class Builder {
      private String message;
      private Builder() {
        // Do nothing.
      }
      public Builder setMessage(String message) {
        this.message = message;
        return this;
      }
      public RequestMessage build() {
        return new RequestMessage(message);
      }
    }
    public static Builder newBuilder() {
      return new Builder();
    }
  }

  {
    //#expression
    Function<Session, RequestMessage> request = session -> {
      String message = session.getString("message");
      return RequestMessage.newBuilder()
        .setMessage(message)
        .build();
    };
    //#expression
  }
}
