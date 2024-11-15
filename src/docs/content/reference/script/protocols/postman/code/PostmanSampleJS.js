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
import { postman } from "@gatling.io/postman";
//#imports

//#fromResource
const collection = postman.fromResource("My Collection.postman_collection.json");
//#fromResource

//#environment
// Import environment from exported JSON file:
collection.environment("My Environment.postman_environment.json");
// Import environment from a JavaScript object:
collection.environment({ "key 1": "value 1" });
//#environment

//#globals
// Import global variables from exported JSON file:
collection.globals("My Globals.postman_globals.json");
// Import global variables from a JavaScript object:
collection.globals({ "key 1": "value 1" });
//#globals

//#folder
collection.folder("My Folder");
// Refers to the request "My Request" inside the folder "My Folder":
collection.folder("My Folder").request("My Request");
//#folder

//#scenario
collectionOrFolder.scenario("My Scenario");
// Add pauses between requests (duration in seconds):
collectionOrFolder.scenario("My Scenario", { pauses: 1 });
// Add pauses between requests (specify time unit):
collectionOrFolder.scenario("My Scenario", { pauses: { amount: 1, unit: "seconds" }});
// By default, only requests defined directly at the current level are included.
// Use the `recursive` option if you want to include all requests defined in
// sub-folders (at any depth):
collectionOrFolder.scenario("My Scenario", { recursive: true });
//#scenario

//#request
collectionOrFolder.request("My Request");
// Override the request name shown in Gatling reports:
collectionOrFolder.request("My Request", "Overridden Request Name");
//#request
