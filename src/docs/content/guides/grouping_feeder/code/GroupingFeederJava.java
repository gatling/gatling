/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import io.gatling.javaapi.core.ChainBuilder;

import java.util.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

class GroupingFeederJava {
//#grouping-feeder
List<Map<String, Object>> records = csv("file.csv").readRecords();

Map<String, List<Map<String, Object>>> recordsGroupedByUsername =
  records
    .stream()
    .collect(java.util.stream.Collectors.groupingBy(record -> (String) record.get("username")));

Iterator<Map<String, Object>> groupedRecordsFeeder =
  recordsGroupedByUsername
    .values()
    .stream()
    .map(groupedRecords -> Collections.singletonMap("userRecords", (Object) groupedRecords))
    .iterator();

ChainBuilder chain =
  feed(groupedRecordsFeeder)
    .foreach("#{userRecords}", "record").on(
      exec(http("request").get("${record.url}"))
    );
//#grouping-feeder
}