import com.typesafe.sbt.SbtPgp.PgpKeys._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._

object Release {

  lazy val settings = releaseSettings ++ Seq(
    crossBuild := false,
    publishArtifactsAction := publishSigned.value
  )

}
