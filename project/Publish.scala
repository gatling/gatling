import sbt._
import sbt.Keys._

import aether.Aether.aetherPublishSettings

object Publish {

  /*************************/
  /** Publishing settings **/
  /*************************/

  lazy val settings = aetherPublishSettings ++ Seq(
    crossPaths           := false,
    pomExtra             := scm ++ developersXml(developers),
    publishMavenStyle    := true,
    pomIncludeRepository := { _ => false },
    publishTo            := Some(if(isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
    credentials          += Credentials(Path.userHome / ".sbt" / ".credentials")
  )

  /************************/
  /** POM extra metadata **/
  /************************/

  private val scm = {
    <scm>
      <connection>scm:git:git@github.com:gatling/gatling.git</connection>
      <developerConnection>scm:git:git@github.com:gatling/gatling.git</developerConnection>
      <url>https://github.com/gatling/gatling</url>
      <tag>HEAD</tag>
    </scm>
  }

  private case class GatlingDeveloper(emailAddress: String, name: String, isEbiz: Boolean)

  private val developers = Seq(
    GatlingDeveloper("slandelle@excilys.com", "Stephane Landelle", true),
    GatlingDeveloper("nremond@gmail.com", "Nicolas Rémond", false),
    GatlingDeveloper("pdalpra@excilys.com", "Pierre Dal-Pra", true),
    GatlingDeveloper("aduffy@gilt.com", "Andrew Duffy", false),
    GatlingDeveloper("jasonk@bluedevel.com", "Jason Koch", false),
    GatlingDeveloper("ivan.mushketik@gmail.com", "Ivan Mushketyk", false),
    GatlingDeveloper("gcorre@excilys.com", "Guillaume Corré", true)
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
