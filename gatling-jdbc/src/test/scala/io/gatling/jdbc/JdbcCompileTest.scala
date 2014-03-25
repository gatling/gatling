/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.jdbc

import io.gatling.core.Predef._
import io.gatling.jdbc.Predef._

class JdbcCompileTest extends Simulation {

  val testData2 = jdbcFeeder("jdbc:postgresql:gatling", "gatling", "gatling", """
select login as "username", password
from usr
where id in (select usr_id from usr_role where role_id='ROLE_USER')
and id not in (select usr_id from usr_role where role_id='ROLE_ADMIN')
and (select count(*) from usr_account where usr_id=id) >=2""")

}
