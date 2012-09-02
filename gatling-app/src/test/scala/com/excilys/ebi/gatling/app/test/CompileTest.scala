/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.app.test
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import com.ning.http.client.{ Response, Request }

object CompileTest {

	val iterations = 10
	val pause1 = 1
	val pause2 = 2
	val pause3 = 3

	val baseUrl = "http://localhost:3000"

	val httpConf = httpConfig.baseURL(baseUrl).proxy("91.121.211.157", 80).httpsPort(4443).credentials("rom", "test")

	val httpConf2 = httpConfig
		.baseURL("http://172.30.5.143:8080")
		.proxy("172.31.76.106", 8080)
		.httpsPort(8081)
		.acceptHeader("*/*")
		.acceptCharsetHeader("ISO-8859-1,utf-8;q=0.7,*;q=0.3")
		.acceptLanguageHeader("fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4")
		.acceptEncodingHeader("gzip,deflate,sdch")
		.userAgentHeader("Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/12.04 Chromium/18.0.1025.151 Chrome/18.0.1025.151 Safari/535.19")
		.hostHeader("172.30.5.143:8080")

	val httpConfToVerifyUserProvidedInfoExtractors = httpConfig
		.requestInfoExtractor((request: Request) => { List[String]() })
		.responseInfoExtractor((response: Response) => { List[String]() })

	val usersInformation = tsv("user_information.tsv")

	val loginChain = chain.exec(http("First Request Chain").get("/")).pause(1, 2)

	val testData = tsv("test-data.tsv")

	val testData2 = jdbcFeeder("jdbc:postgresql:gatling", "gatling", "gatling", """
select login as "username", password
from usr
where id in (select usr_id from usr_role where role_id='ROLE_USER')
and id not in (select usr_id from usr_role where role_id='ROLE_ADMIN')
and (select count(*) from usr_account where usr_id=id) >=2""")

	val lambdaUser = scenario("Standard User")
		.insertChain(loginChain)
		// First request outside iteration
		.loop(
			chain
				.feed(testData)
				.exec(http("Catégorie Poney").get("/").queryParam("omg").queryParam("socool").basicAuth("", "").check(xpath("//input[@id='text1']/@value").transform(_ + "foo").saveAs("aaaa_value"), jsonPath("//foo/bar[2]/baz"))))
		.times(2)
		.pause(pause2, pause3)
		// Loop
		.loop(
			// What will be repeated ?
			chain
				// First request to be repeated
				.exec((session: Session) => {
					println("iterate: " + session.getCounterValue("titi"))
					session
				})
				.exec(
					http("Page accueil").get("http://localhost:3000")
						.check(
							xpath("//input[@value='${aaaa_value}']/@id").saveAs("sessionParam"),
							xpath("//input[@id='${aaaa_value}']/@value").notExists,
							css(""".foo"""),
							css("""#foo""", "href"),
							css(""".foo""").count.is(1),
							css(""".foo""").notExists,
							regex("""<input id="text1" type="text" value="aaaa" />"""),
							regex("""<input id="text1" type="text" value="aaaa" />""").count.is(1),
							regex("""<input id="text1" type="test" value="aaaa" />""").notExists,
							status.in(200 to 210).saveAs("blablaParam"),
							xpath("//input[@value='aaaa']/@id").not("omg"),
							xpath("//input[@id='text1']/@value").is("aaaa").saveAs("test2"),
							md5.is("0xA59E79AB53EEF2883D72B8F8398C9AC3"),
							responseTimeInMillis.lessThan(1000),
							latencyInMillis.lessThan(1000)))
				.loop(chain
					.exec(http("In During 1").get("http://localhost:3000/aaaa"))
					.pause(2)
					.loop(chain.exec((session: Session) => {
						println("--nested loop: " + session.getCounterValue("tutu"))
						session
					})).counterName("tutu").times(2)
					.exec((session: Session) => {
						println("-loopDuring: " + session.getCounterValue("foo"))
						session
					})
					.exec(http("In During 2").get("/"))
					.pause(2))
				.counterName("foo").during(12000, MILLISECONDS)
				.pause(pause2)
				.loop(
					chain
						.exec(http("In During 1").get("/"))
						.pause(2)
						.exec((session: Session) => {
							println("-iterate1: " + session.getCounterValue("titi") + ", doFor: " + session.getCounterValue("hehe"))
							session
						})
						.loop(
							chain
								.exec((session: Session) => {
									println("--iterate1: " + session.getCounterValue("titi") + ", doFor: " + session.getCounterValue("hehe") + ", iterate2: " + session.getCounterValue("hoho"))
									session
								}))
						.counterName("hoho").times(2)
						.exec(http("In During 2").get("/"))
						.pause(2))
				.counterName("hehe").during(12000, MILLISECONDS)
				.exec((session: Session) => session.setAttribute("test2", "bbbb"))
				.doIf("test2", "aaaa",
					chain.exec(http("IF=TRUE Request").get("/")), chain.exec(http("IF=FALSE Request").get("/")))
				.pause(pause2)
				.exec(http("Url from session").get("/aaaa"))
				.pause(1000, 3000, MILLISECONDS)
				// Second request to be repeated
				.exec(http("Create Thing blabla").post("/things").queryParam("login").queryParam("password").fileBody("create_thing", Map("name" -> "blabla")).asJSON)
				.pause(pause1)
				// Third request to be repeated
				.exec(http("Liste Articles").get("/things").queryParam("firstname").queryParam("lastname"))
				.pauseExp(pause1)
				.exec(http("Test Page").get("/tests").check(header(CONTENT_TYPE).is("text/html; charset=utf-8").saveAs("sessionParam")))
				// Fourth request to be repeated
				.pauseExp(100, MILLISECONDS)
				// switch
				.randomSwitch(
					40 -> chain.exec(http("Possibility 1").get("/p1")),
					55 -> chain.exec(http("Possibility 2").get("/p2")) // last 5% bypass
				)
				.exec(http("Create Thing omgomg")
					.post("/things").queryParam("postTest", "${sessionParam}").fileBody("create_thing", Map("name" -> "${sessionParam}")).asJSON
					.check(status.is(201).saveAs("status")))).counterName("titi").times(iterations)
		// Head request
		.exec(http("head on root").head("/"))
		// Second request outside iteration
		.exec(http("Ajout au panier").get("/").check(regex("""<input id="text1" type="text" value="(.*)" />""").saveAs("input")))
		.pause(pause1)

	val config = lambdaUser.configure.users(5).ramp(10).protocolConfig(httpConf)
}
