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

/*
//#random-mail-generator
The `feeder(Iterator)` method is currently not supported by Gatling JS.
//#random-mail-generator
*/

//#feed-keyword
feed(feeder);
//#feed-keyword

//#feed-multiple
// feed 2 records at once
feed(feeder, 2);
// feed a number of records that's defined as the "numberOfRecords" attribute
// stored in the session of the virtual user
feed(feeder, "#{numberOfRecords}");
// feed a number of records that's computed dynamically from the session
// with a function
feed(feeder, (session) => session.get("numberOfRecords"));
//#feed-multiple

//#strategies
// default behavior: use an Iterator on the underlying sequence
csv("foo").queue();
// randomly pick an entry in the sequence
csv("foo").random();
// shuffle entries, then behave like queue
csv("foo").shuffle();
// go back to the top of the sequence once the end is reached
csv("foo").circular();
//#strategies

//#feeder-in-memory
// using an array
arrayFeeder([
  { "foo": "foo1" },
  { "foo": "foo2" },
  { "foo": "foo3" }
]).random();
//#feeder-in-memory

//#sep-values-feeders
// use a comma separator
csv("foo.csv");
// use a tabulation separator
tsv("foo.tsv");
// use a semicolon separator
ssv("foo.ssv");
// use a custom separator
separatedValues("foo.txt", '#');
//#sep-values-feeders

//#eager
csv("foo.csv").eager().random();
//#eager

//#batch
// use default buffer size (2000 lines)
csv("foo.csv").batch().random();
// tune internal buffer size
csv("foo.csv").batch(200).random();
//#batch

//#unzip
csv("foo.csv.zip").unzip();
//#unzip

//#shard
csv("foo.csv").shard();
//#shard

//#json-feeders
jsonFile("foo.json");
jsonUrl("http://me.com/foo.json");
//#json-feeders

/*
//#jdbc-feeder
NOT SUPPORTED
//#jdbc-feeder
*/

//#sitemap-imports
// beware: you need to import the http module
import { sitemap } from "@gatling.io/http";
//#sitemap-imports

//#sitemap-feeder
sitemap("/path/to/sitemap/file");
//#sitemap-feeder

/*
//#redis-LPOP
NOT SUPPORTED
//#redis-LPOP
*/

/*
//#redis-SPOP
NOT SUPPORTED
//#redis-SPOP
*/

/*
//#redis-SRANDMEMBER
NOT SUPPORTED
//#redis-SRANDMEMBER
*/

/*
//#redis-RPOPLPUSH
NOT SUPPORTED
//#redis-RPOPLPUSH
*/

//#transform
csv("myFile.csv").transform((key, value) =>
  key === "attributeThatShouldBeAnInt" ? parseInt(value) : value
);
//#transform

/*
//#records
NOT SUPPORTED
//#records
*/

//#recordsCount
const recordsCount = csv("myFile.csv").recordsCount();
//#recordsCount
