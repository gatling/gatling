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

import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.gatling.javaapi.http.HttpDsl.http;

class DynatraceSampleJava {

//#dynatrace-sample
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
  });
//#dynatrace-sample
}
