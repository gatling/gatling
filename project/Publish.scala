import sbt._
import sbt.Keys._

import Resolvers._

object Publish {

  /*************************/
  /** Publishing settings **/
  /*************************/

  lazy val settings = Seq(
    crossPaths           := false,
    pomExtra             := scm ++ developersXml(developers),
    publishMavenStyle    := true,
    pomIncludeRepository := { _ => false },
    publishTo            := Some(if(isSnapshot.value) sonatypeSnapshots else sonatypeStaging),
    credentials          += Credentials(Path.userHome / ".sbt" / ".credentials")
  )

  /************************/
  /** POM extra metadata **/
  /************************/

  private val scm = {
    <scm>
      <connection>scm:git:git@github.com:excilys/gatling.git</connection>
      <developerConnection>scm:git:git@github.com:excilys/gatling.git</developerConnection>
      <url>https://github.com/excilys/gatling</url>
      <tag>HEAD</tag>
    </scm>
  }

  private case class GatlingDeveloper(emailAddress: String, name: String, isEbiz: Boolean)

  private val developers = Seq(
    GatlingDeveloper("slandelle@excilys.com", "Stephane Landelle", true),
    GatlingDeveloper("rsertelon@excilys.com", "Romain Sertelon", true),
    GatlingDeveloper("ybenkhaled@excilys.com", "Yassine Ben Khaled", true),
    GatlingDeveloper("hcordier@excilys.com", "Hugo Cordier", true),
    GatlingDeveloper("nremond@gmail.com", "Nicolas Rémond", false),
    GatlingDeveloper("skuenzli@gmail.com", "Stephen Kuenzli", false),
    GatlingDeveloper("pdalpra@excilys.com", "Pierre Dal-Pra", true),
    GatlingDeveloper("gcoutant@excilys.com", "Grégory Coutant", true),
    GatlingDeveloper("blemale@excilys.com", "Bastien Lemale", true),
    GatlingDeveloper("aduffy@gilt.com", "Andrew Duffy", false),
    GatlingDeveloper("jasonk@bluedevel.com", "Jason Koch", false)
  )

  private def developersXml(devs: Seq[GatlingDeveloper]) = {
    <developers>
    {
      for(dev <- devs)
      yield {
        <developer>
          <id>{dev.emailAddress}</id>
          <name>{dev.name}</name>
          { if (dev.isEbiz) <organization>eBusiness Information, Excilys Group</organization> }
        </developer>
      }
    }
    </developers>
  }
}
